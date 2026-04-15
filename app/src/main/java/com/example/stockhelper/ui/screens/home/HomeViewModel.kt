package com.example.stockhelper.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockhelper.data.local.AppDatabase
import com.example.stockhelper.data.repository.StockRepository
import com.example.stockhelper.data.repository.StockRepositoryImpl
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
    private val notificationHelper: NotificationHelper

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var alertedStocks = mutableSetOf<String>()
    private var reachedConditions = mutableSetOf<String>() // 跟踪第一步是否已达成

    init {
        val database = AppDatabase.getDatabase(application)
        repository = StockRepositoryImpl(database.stockDao())
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

    private fun checkAlerts(stocks: List<Stock>, quotes: Map<String, StockQuote>) {
        stocks.forEach { stock ->
            val quote = quotes[stock.code] ?: return@forEach
            val currentPrice = quote.currentPrice
            val isBuyFirst = stock.tradeType == "BUY_FIRST"

            // 检查第一步是否达成
            val buyReached = currentPrice <= stock.targetBuyPrice
            val sellReached = currentPrice >= stock.targetSellPrice
            val firstStepKey = "${stock.code}_first_reached"

            if (isBuyFirst) {
                // 先买后卖模式：买入价达成后才检查卖出提醒
                if (buyReached) {
                    reachedConditions.add(firstStepKey)
                }

                // 买入提醒：当前价格 <= 目标买入价
                if (buyReached && !alertedStocks.contains("${stock.code}_buy")) {
                    alertedStocks.add("${stock.code}_buy")
                    notificationHelper.sendPriceAlert(
                        stockCode = stock.code,
                        stockName = stock.name,
                        message = "【买入提醒】${stock.name}(${stock.code}) 当前价格 ${String.format("%.2f", currentPrice)} 元，已达到目标买入价 ${String.format("%.2f", stock.targetBuyPrice)} 元",
                        isBuy = true
                    )
                }

                // 卖出提醒：当前价格 >= 目标卖出价 且 买入价已达成
                if (sellReached && reachedConditions.contains(firstStepKey) && !alertedStocks.contains("${stock.code}_sell")) {
                    alertedStocks.add("${stock.code}_sell")
                    val tShares = getTShares(stock)
                    val expectedProfit = (stock.targetSellPrice - stock.targetBuyPrice) * tShares * 0.999
                    notificationHelper.sendPriceAlert(
                        stockCode = stock.code,
                        stockName = stock.name,
                        message = "【卖出提醒】${stock.name}(${stock.code}) 当前价格 ${String.format("%.2f", currentPrice)} 元，已达到目标卖出价 ${String.format("%.2f", stock.targetSellPrice)} 元，预估收益 ${String.format("%.2f", expectedProfit)} 元",
                        isBuy = false
                    )
                }
            } else {
                // 先卖后买模式：卖出价达成后才检查买入提醒
                if (sellReached) {
                    reachedConditions.add(firstStepKey)
                }

                // 卖出提醒：当前价格 >= 目标卖出价
                if (sellReached && !alertedStocks.contains("${stock.code}_sell")) {
                    alertedStocks.add("${stock.code}_sell")
                    val tShares = getTShares(stock)
                    val expectedProfit = (stock.targetSellPrice - stock.targetBuyPrice) * tShares * 0.999
                    notificationHelper.sendPriceAlert(
                        stockCode = stock.code,
                        stockName = stock.name,
                        message = "【卖出提醒】${stock.name}(${stock.code}) 当前价格 ${String.format("%.2f", currentPrice)} 元，已达到目标卖出价 ${String.format("%.2f", stock.targetSellPrice)} 元，预估收益 ${String.format("%.2f", expectedProfit)} 元",
                        isBuy = false
                    )
                }

                // 买入提醒：当前价格 <= 目标买入价 且 卖出价已达成
                if (buyReached && reachedConditions.contains(firstStepKey) && !alertedStocks.contains("${stock.code}_buy")) {
                    alertedStocks.add("${stock.code}_buy")
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
            alertedStocks.remove("${code}_buy")
            alertedStocks.remove("${code}_sell")
            reachedConditions.remove("${code}_first_reached")
        }
    }

    fun getStocksWithQuotes(): List<StockWithQuote> {
        val state = _uiState.value
        return state.stocks.map { stock ->
            val quote = state.quotes[stock.code]
            val isBuyFirst = stock.tradeType == "BUY_FIRST"
            val firstStepKey = "${stock.code}_first_reached"

            val shouldBuyAlert = quote?.currentPrice?.let { currentPrice ->
                val buyReached = currentPrice <= stock.targetBuyPrice
                if (isBuyFirst) {
                    // 先买后卖：买入达成时才显示买入提醒
                    buyReached
                } else {
                    // 先卖后买：卖出达成后才显示买入提醒
                    buyReached && reachedConditions.contains(firstStepKey)
                }
            } ?: false

            val shouldSellAlert = quote?.currentPrice?.let { currentPrice ->
                val sellReached = currentPrice >= stock.targetSellPrice
                if (isBuyFirst) {
                    // 先买后卖：买入达成后才显示卖出提醒
                    sellReached && reachedConditions.contains(firstStepKey)
                } else {
                    // 先卖后买：卖出达成时才显示卖出提醒
                    sellReached
                }
            } ?: false

            val expectedProfit = if (quote != null) {
                val tShares = getTShares(stock)
                (stock.targetSellPrice - stock.targetBuyPrice) * tShares * 0.999
            } else null

            val profitRate = if (quote != null && stock.targetBuyPrice > 0) {
                ((stock.targetSellPrice - stock.targetBuyPrice) / stock.targetBuyPrice) * 100 * 0.999
            } else null

            StockWithQuote(
                stock = stock,
                quote = quote,
                expectedProfit = expectedProfit,
                profitRate = profitRate,
                shouldBuyAlert = shouldBuyAlert,
                shouldSellAlert = shouldSellAlert
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }
}
