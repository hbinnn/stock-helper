package com.example.stockhelper.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trade_records")
data class TradeRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val stockCode: String,
    val stockName: String,
    val recordType: String,        // BUY_ALERT / SELL_ALERT
    val currentPrice: Double,      // 触发时价格
    val targetPrice: Double,       // 目标价格
    val tradeType: String,         // BUY_FIRST / SELL_FIRST
    val tShares: Int,             // 做T股数
    val triggeredAt: Long = System.currentTimeMillis(),
    val isHandled: Boolean = false // 用户是否已处理
)
