package com.example.stockhelper.domain.model

data class StockQuote(
    val code: String,
    val name: String,
    val currentPrice: Double,
    val changePercent: Double,
    val high: Double,
    val low: Double,
    val open: Double,
    val previousClose: Double
)
