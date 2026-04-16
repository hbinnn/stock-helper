package com.example.stockhelper.data.repository

import com.example.stockhelper.data.local.StockDao
import com.example.stockhelper.data.remote.EastMoneyApi
import com.example.stockhelper.data.remote.NetworkModule
import com.example.stockhelper.domain.model.Stock
import com.example.stockhelper.domain.model.StockQuote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.Request

class StockRepositoryImpl(
    private val stockDao: StockDao
) : StockRepository {

    private val api: EastMoneyApi = NetworkModule.eastMoneyApi
    private val tencentClient = NetworkModule.tencentHttpClient

    override fun getAllStocks(): Flow<List<Stock>> {
        return stockDao.getAllStocks()
    }

    override suspend fun getStockByCode(code: String): Stock? {
        return stockDao.getStockByCode(code)
    }

    override suspend fun addStock(stock: Stock) {
        stockDao.insertStock(stock)
    }

    override suspend fun deleteStock(code: String) {
        stockDao.deleteStockByCode(code)
    }

    override suspend fun getStockQuote(stock: Stock): Result<StockQuote> {
        return withContext(Dispatchers.IO) {
            // 首先尝试东方财富API，失败则使用腾讯财经API
            try {
                val result = getStockQuoteFromEastMoney(stock)
                if (result.isSuccess) {
                    return@withContext result
                }
            } catch (e: Exception) {
                // 东方财富异常，尝试腾讯API
            }
            // 尝试腾讯财经API
            getStockQuoteFromTencent(stock)
        }
    }

    private suspend fun getStockQuoteFromEastMoney(stock: Stock): Result<StockQuote> {
        val secid = getSecid(stock.code, stock.market)
        val response = api.getStockQuote(secid)
        val data = response.data ?: return Result.failure(Exception("东方财富API返回空数据"))

        val currentPrice = (data.f43?.toDoubleOrNull() ?: 0.0) / 100.0
        val changePercent = (data.f170?.toDoubleOrNull() ?: 0.0) / 100.0
        val high = (data.f44?.toDoubleOrNull() ?: 0.0) / 100.0
        val low = (data.f45?.toDoubleOrNull() ?: 0.0) / 100.0
        val open = (data.f46?.toDoubleOrNull() ?: 0.0) / 100.0
        val previousClose = (data.f60?.toDoubleOrNull() ?: 0.0) / 100.0

        return Result.success(
            StockQuote(
                code = stock.code,
                name = data.f58 ?: stock.name,
                currentPrice = currentPrice,
                changePercent = changePercent,
                high = high,
                low = low,
                open = open,
                previousClose = previousClose
            )
        )
    }

    private suspend fun getStockQuoteFromTencent(stock: Stock): Result<StockQuote> {
        return withContext(Dispatchers.IO) {
            try {
                val market = if (stock.market == "sh") "sh" else "sz"
                val url = "${TencentApi.BASE_URL}q=$market${stock.code}"

                val request = Request.Builder()
                    .url(url)
                    .build()

                val response = tencentClient.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("腾讯API返回为空"))

                // 解析腾讯财经数据格式: v_sh600519="1~贵州茅台~600519~1462.84~...";  (注意末尾有分号)
                val regex = """v_\w+\d+="([^"]+)";?""".toRegex()
                val match = regex.find(body) ?: return@withContext Result.failure(Exception("腾讯API解析失败"))

                val fields = match.groupValues[1].split("~")
                if (fields.size < 33) {
                    return@withContext Result.failure(Exception("腾讯API数据格式错误"))
                }

                val name = fields[1]
                val currentPrice = fields[3].toDoubleOrNull() ?: 0.0
                val previousClose = fields[4].toDoubleOrNull() ?: 0.0
                val open = fields[5].toDoubleOrNull() ?: 0.0
                val volume = fields[6].toDoubleOrNull() ?: 0.0 // 成交量(手)
                val high = fields[33].toDoubleOrNull() ?: 0.0
                val low = fields[34].toDoubleOrNull() ?: 0.0
                val change = fields[31].toDoubleOrNull() ?: 0.0
                val changePercent = fields[32].toDoubleOrNull() ?: 0.0

                Result.success(
                    StockQuote(
                        code = stock.code,
                        name = name,
                        currentPrice = currentPrice,
                        changePercent = changePercent,
                        high = high,
                        low = low,
                        open = open,
                        previousClose = previousClose
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun refreshQuotes(stocks: List<Stock>): Map<String, Result<StockQuote>> {
        return withContext(Dispatchers.IO) {
            stocks.associate { stock ->
                stock.code to getStockQuote(stock)
            }
        }
    }

    private fun getSecid(code: String, market: String): String {
        return if (market == "sh") {
            "1.$code"
        } else {
            "0.$code"
        }
    }
}

object TencentApi {
    const val BASE_URL = "http://qt.gtimg.cn/"
}
