package com.example.stockhelper.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stocks")
data class Stock(
    @PrimaryKey
    val code: String,
    val name: String,
    val costPrice: Double,  // 成本价
    val targetBuyPrice: Double,  // 目标买入价
    val targetSellPrice: Double, // 目标卖出价
    val buyPriceType: String = "PRICE",  // PRICE=固定价格, PERCENT=百分比
    val sellPriceType: String = "PRICE", // PRICE=固定价格, PERCENT=百分比
    val buyPercent: Double = 0.0,  // 百分比时使用
    val sellPercent: Double = 0.0, // 百分比时使用
    val shares: Int,  // 持仓数量
    val market: String = "sh", // sh=上海, sz=深圳
    val tradeType: String = "BUY_FIRST", // BUY_FIRST=先买后卖, SELL_FIRST=先卖后买
    val tTradeType: String = "PERCENT", // SHARES=固定股数, PERCENT=百分比
    val tShares: Int = 0, // 固定做T股数
    val tSharesPercent: Int = 25 // 做T比例（25=1/4, 33=1/3等）
)
