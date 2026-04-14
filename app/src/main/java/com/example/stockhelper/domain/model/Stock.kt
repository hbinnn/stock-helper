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
    val shares: Int,
    val market: String = "sh" // sh=上海, sz=深圳
)
