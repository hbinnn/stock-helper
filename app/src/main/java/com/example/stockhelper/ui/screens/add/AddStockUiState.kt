package com.example.stockhelper.ui.screens.add

data class AddStockUiState(
    val market: String = "sh",
    val code: String = "",
    val name: String = "",
    val fetchedName: String? = null,
    val currentPrice: Double? = null,
    val changePercent: Double? = null,
    val isLoadingInfo: Boolean = false,

    // 成本价相关
    val costPrice: Double? = null,  // 成本价（从现价获取或用户输入）
    val costPriceInput: String = "",  // 用户输入的成本价

    // 目标买入价相关
    val targetBuyPrice: String = "",
    val buyPriceType: String = "PRICE",  // PRICE=固定价格, PERCENT=百分比
    val buyPercent: String = "",  // 百分比输入

    // 目标卖出价相关
    val targetSellPrice: String = "",
    val sellPriceType: String = "PRICE",  // PRICE=固定价格, PERCENT=百分比
    val sellPercent: String = "",  // 百分比输入

    val shares: String = "",
    val tradeType: String = "BUY_FIRST",  // BUY_FIRST=先买后卖, SELL_FIRST=先卖后买
    val tTradeType: String = "PERCENT",    // SHARES=固定股数, PERCENT=百分比
    val tShares: String = "",               // 固定做T股数
    val tSharesPercent: String = "25",      // 做T比例
    val error: String? = null,
    val isSaved: Boolean = false
)
