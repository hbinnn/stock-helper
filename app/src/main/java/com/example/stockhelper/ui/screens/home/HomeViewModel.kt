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

    init {
        val database = AppDatabase.getDatabase(application)
        repository = StockRepositoryImpl(database.stockDao())
        notificationHelper = NotificationHelper(application)

        loadStocks()
        startAutoRefresh()
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

            // 买入提醒：当前价格 <= 目标买入价
            if (currentPrice <= stock.targetBuyPrice && !alertedStocks.contains("${stock.code}_buy")) {
                alertedStocks.add("${stock.code}_buy")
                notificationHelper.sendPriceAlert(
                    stockCode = stock.code,
                    stockName = stock.name,
                    message = "【买入提醒】${stock.name}(${stock.code}) 当前价格 ${String.format("%.2f", currentPrice)} 元，已达到目标买入价 ${String.format("%.2f", stock.targetBuyPrice)} 元",
                    isBuy = true
                )
            }

            // 卖出提醒：当前价格 >= 目标卖出价
            if (currentPrice >= stock.targetSellPrice && !alertedStocks.contains("${stock.code}_sell")) {
                alertedStocks.add("${stock.code}_sell")
                val expectedProfit = (stock.targetSellPrice - stock.targetBuyPrice) * stock.shares * 100 * 0.999
                notificationHelper.sendPriceAlert(
                    stockCode = stock.code,
                    stockName = stock.name,
                    message = "【卖出提醒】${stock.name}(${stock.code}) 当前价格 ${String.format("%.2f", currentPrice)} 元，已达到目标卖出价 ${String.format("%.2f", stock.targetSellPrice)} 元，预估收益 ${String.format("%.2f", expectedProfit)} 元",
                    isBuy = false
                )
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
        }
    }

    fun getStocksWithQuotes(): List<StockWithQuote> {
        val state = _uiState.value
        return state.stocks.map { stock ->
            val quote = state.quotes[stock.code]
            val shouldBuyAlert = quote?.currentPrice?.let { it <= stock.targetBuyPrice } ?: false
            val shouldSellAlert = quote?.currentPrice?.let { it >= stock.targetSellPrice } ?: false

            val expectedProfit = if (quote != null) {
                (stock.targetSellPrice - stock.targetBuyPrice) * stock.shares * 100 * 0.999
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
