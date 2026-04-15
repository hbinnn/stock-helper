package com.example.stockhelper.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface EastMoneyApi {
    @GET("api/qt/stock/get")
    suspend fun getStockQuote(
        @Query("secid") secid: String,
        @Query("fields") fields: String = "f43,f44,f45,f46,f47,f57,f58,f60,f169,f170"
    ): EastMoneyResponse
}

data class EastMoneyResponse(
    val data: EastMoneyData?
)

data class EastMoneyData(
    val f43: String?,  // 当前价格（分）
    val f44: String?,  // 最高价
    val f45: String?,  // 最低价
    val f46: String?,  // 今开
    val f47: String?,  // 成交量
    val f57: String?,  // 股票代码
    val f58: String?,  // 股票名称
    val f60: String?,  // 昨收（分）
    val f169: String?, // 涨跌额
    val f170: String?  // 涨跌幅(%)
)
