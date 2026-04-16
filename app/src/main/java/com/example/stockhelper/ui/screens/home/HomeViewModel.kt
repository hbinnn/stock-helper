package com.example.stockhelper.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockhelper.data.local.AlertStateDao
import com.example.stockhelper.data.local.AppDatabase
import com.example.stockhelper.data.repository.StockRepository
import com.example.stockhelper.data.repository.StockRepositoryImpl
import com.example.stockhelper.domain.model.AlertState
import com.example.stockhelper.domain.model.Stock
import com.example.stockhelper.domain.model.StockQuote
import com.example.stockhelper.util.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StockRepository
    private val alertStateDao: AlertStateDao
    private val notificationHelper: NotificationHelper

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = StockRepositoryImpl(database.stockDao())
        alertStateDao = database.alertStateDao()
        notificationHelper = NotificationHelper(application)

        loadStocks()
        startAutoRefresh()
    }

    // 计算做T股数
    private fun getTShares(stock: Stock): Int {
        return if (stock.tTradeType == "PERCENT") {
            (stock.shares * stock.tSharesPercent / 100)
        } else {
            stock.tShares
        }
    }

    private fun loadStocks() {
        viewModelScope.launch {
            repository.getAllStocks().collect { stocks ->
                _uiState.update { it.copy(stocks = stocks) }
                refreshQuotes(stocks)
            }
        }
    }

    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                delay(15000) // 15秒刷新一次
                val currentStocks = _uiState.value.stocks
                if (currentStocks.isNotEmpty()) {
                    refreshQuotes(currentStocks)
                }
            }
        }
    }

    private suspend fun refreshQuotes(stocks: List<Stock>) {
        _uiState.update { it.copy(isLoading = true) }
        val results = repository.refreshQuotes(stocks)
        val quotesMap = mutableMapOf<String, StockQuote>()

        results.forEach { (code, result) ->
            result.onSuccess { quote ->
                quotesMap[code] = quote
            }
        }

        _uiState.update { state ->
            state.copy(
                quotes = quotesMap,
                isLoading = false,
                error = if (quotesMap.isEmpty() && stocks.isNotEmpty()) "无法获取行情数据" else null
            )
        }

        // 检查是否需要提醒
        checkAlerts(stocks, quotesMap)
    }

    private suspend fun checkAlerts(stocks: List<Stock>, quotes: Map<String, StockQuote>) {
        stocks.forEach { stock ->
            val quote = quotes[stock.code] ?: return@forEach
            val currentPrice = quote.currentPrice
            val isBuyFirst = stock.tradeType == "BUY_FIRST"

            var alertState = alertStateDao.getAlertState(stock.code)
            if (alertState == null) {
                alertState = AlertState(stockCode = stock.code)
                alertStateDao.insertOrUpdate(alertState)
            }

            val buyReached = currentPrice <= stock.targetBuyPrice
            val sellReached = currentPrice >= stock.targetSellPrice
            val tShares = getTShares(stock)

            if (isBuyFirst) {
                // 先买后卖模式
                if (buyReached && !alertState.firstConditionReached) {
                    alertState = alertState.copy(
                        firstConditionReached = true,
                        firstConditionReachedAt = System.currentTimeMillis()
                    )
                    alertStateDao.insertOrUpdate(alertState)
                    notificationHelper.sendPriceAlert(
                        stockCode = stock.code,
                        stockName = stock.name,
                        message = "【买入提醒】${stock.name}(${stock.code}) 当前价格 ${String.format("%.2f", currentPrice)} 元，已达到目标买入价 ${String.format("%.2f", stock.targetBuyPrice)} 元",
                        isBuy = true
                    )
                }

                if (sellReached && alertState.firstConditionReached && !alertState.sellAlertSent) {
                    alertState = alertState.copy(sellAlertSent = true)
                    alertStateDao.insertOrUpdate(alertState)
                    val expectedProfit = (stock.targetSellPrice - stock.targetBuyPrice) * tShares * 0.999
                    notificationHelper.sendPriceAlert(
                        stockCode = stock.code,
                        stockName = stock.name,
                        message = "【卖出提醒】${stock.name}(${stock.code}) 当前价格 ${String.format("%.2f", currentPrice)} 元，已达到目标卖出价 ${String.format("%.2f", stock.targetSellPrice)} 元，预估收益 ${String.format("%.2f", expectedProfit)} 元",
                        isBuy = false
                    )
                }
            } else {
                // 先卖后买模式
                if (sellReached && !alertState.firstConditionReached) {
                    alertState = alertState.copy(
                        firstConditionReached = true,
                        firstConditionReachedAt = System.currentTimeMillis()
                    )
                    alertStateDao.insertOrUpdate(alertState)
                    val expectedProfit = (stock.targetSellPrice - stock.targetBuyPrice) * tShares * 0.999
                    notificationHelper.sendPriceAlert(
                        stockCode = stock.code,
                        stockName = stock.name,
                        message = "【卖出提醒】${stock.name}(${stock.code}) 当前价格 ${String.format("%.2f", currentPrice)} 元，已达到目标卖出价 ${String.format("%.2f", stock.targetSellPrice)} 元，预估收益 ${String.format("%.2f", expectedProfit)} 元",
                        isBuy = false
                    )
                }

                if (buyReached && alertState.firstConditionReached && !alertState.buyAlertSent) {
                    alertState = alertState.copy(buyAlertSent = true)
                    alertStateDao.insertOrUpdate(alertState)
                    notificationHelper.sendPriceAlert(
                        stockCode = stock.code,
                        stockName = stock.name,
                        message = "【买入提醒】${stock.name}(${stock.code}) 当前价格 ${String.format("%.2f", currentPrice)} 元，已达到目标买入价 ${String.format("%.2f", stock.targetBuyPrice)} 元",
                        isBuy = true
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val stocks = _uiState.value.stocks
            if (stocks.isNotEmpty()) {
                refreshQuotes(stocks)
            }
        }
    }

    fun deleteStock(code: String) {
        viewModelScope.launch {
            repository.deleteStock(code)
            alertStateDao.deleteByCode(code)
        }
    }

    fun getStocksWithQuotes(): List<StockWithQuote> {
        val state = _uiState.value
        return state.stocks.map { stock ->
            val quote = state.quotes[stock.code]

            val expectedProfit = if (quote != null) {
                val tShares = getTShares(stock)
                (stock.targetSellPrice - stock.targetBuyPrice) * tShares * 0.999
            } else null

            val profitRate = if (quote != null && stock.targetBuyPrice > 0) {
                ((stock.targetSellPrice - stock.targetBuyPrice) / stock.targetBuyPrice) * 100 * 0.999
            } else null

            // 注意：提醒状态现在从数据库读取，这里简化处理
            // 前端显示的提醒状态以Service和checkAlerts中的发送逻辑为准
            StockWithQuote(
                stock = stock,
                quote = quote,
                expectedProfit = expectedProfit,
                profitRate = profitRate,
                shouldBuyAlert = false,
                shouldSellAlert = false
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }
}
