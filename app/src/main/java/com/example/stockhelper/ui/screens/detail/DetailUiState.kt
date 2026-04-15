package com.example.stockhelper.ui.screens.detail

import com.example.stockhelper.domain.model.StockQuote

data class DetailUiState(
    val stockCode: String = "",
    val stockName: String = "",
    val shares: Int = 0,
    val targetBuyPrice: Double = 0.0,
    val targetSellPrice: Double = 0.0,
    val quote: StockQuote? = null,
    val isLoading: Boolean = false,
    val expectedProfit: Double = 0.0,
    val profitRate: Double = 0.0,
    val tradeType: String = "BUY_FIRST",
    val tTradeType: String = "PERCENT",
    val tShares: Int = 0,
    val tSharesPercent: Int = 25
)
