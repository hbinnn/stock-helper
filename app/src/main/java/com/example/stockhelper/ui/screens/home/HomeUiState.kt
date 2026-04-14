package com.example.stockhelper.ui.screens.home

import com.example.stockhelper.domain.model.Stock
import com.example.stockhelper.domain.model.StockQuote

data class HomeUiState(
    val stocks: List<Stock> = emptyList(),
    val quotes: Map<String, StockQuote> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class StockWithQuote(
    val stock: Stock,
    val quote: StockQuote?,
    val expectedProfit: Double?,
    val profitRate: Double?,
    val shouldBuyAlert: Boolean,
    val shouldSellAlert: Boolean
)
