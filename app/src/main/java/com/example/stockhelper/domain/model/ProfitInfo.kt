package com.example.stockhelper.domain.model

data class ProfitInfo(
    val expectedProfit: Double,
    val profitRate: Double,
    val buyAlert: Boolean,
    val sellAlert: Boolean
)
