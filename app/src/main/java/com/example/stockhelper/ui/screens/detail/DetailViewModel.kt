package com.example.stockhelper.ui.screens.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockhelper.data.local.AppDatabase
import com.example.stockhelper.data.repository.StockRepository
import com.example.stockhelper.data.repository.StockRepositoryImpl
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StockRepository
    private val tradeRecordDao = AppDatabase.getDatabase(application).tradeRecordDao()

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = StockRepositoryImpl(database.stockDao())
    }

    fun loadStock(code: String) {
        viewModelScope.launch {
            val stock = repository.getStockByCode(code)
            if (stock != null) {
                _uiState.update {
                    it.copy(
                        stockCode = stock.code,
                        stockName = stock.name,
                        shares = stock.shares,
                        targetBuyPrice = stock.targetBuyPrice,
                        targetSellPrice = stock.targetSellPrice,
                        tradeType = stock.tradeType,
                        tTradeType = stock.tTradeType,
                        tShares = stock.tShares,
                        tSharesPercent = stock.tSharesPercent
                    )
                }
                loadQuote(stock)
                startAutoRefresh(stock)
                loadTradeRecords(stock.code)
            }
        }
    }

    private fun loadTradeRecords(stockCode: String) {
        viewModelScope.launch {
            tradeRecordDao.getRecordsByStock(stockCode).collect { records ->
                _uiState.update { it.copy(tradeRecords = records) }
            }
        }
    }

    private suspend fun loadQuote(stock: com.example.stockhelper.domain.model.Stock) {
        _uiState.update { it.copy(isLoading = true) }
        repository.getStockQuote(stock).onSuccess { quote ->
            // 计算做T股数
            val tSharesValue = if (stock.tTradeType == "PERCENT") {
                (stock.shares * stock.tSharesPercent / 100)
            } else {
                stock.tShares
            }
            val expectedProfit = (stock.targetSellPrice - stock.targetBuyPrice) * tSharesValue * 0.999
            val profitRate = if (stock.targetBuyPrice > 0) {
                ((stock.targetSellPrice - stock.targetBuyPrice) / stock.targetBuyPrice) * 100 * 0.999
            } else 0.0

            _uiState.update {
                it.copy(
                    quote = quote,
                    expectedProfit = expectedProfit,
                    profitRate = profitRate,
                    isLoading = false
                )
            }
        }.onFailure {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun startAutoRefresh(stock: com.example.stockhelper.domain.model.Stock) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                delay(15000)
                loadQuote(stock)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }
}
