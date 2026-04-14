package com.example.stockhelper.data.repository

import com.example.stockhelper.data.local.StockDao
import com.example.stockhelper.data.remote.EastMoneyApi
import com.example.stockhelper.data.remote.NetworkModule
import com.example.stockhelper.domain.model.Stock
import com.example.stockhelper.domain.model.StockQuote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class StockRepositoryImpl(
    private val stockDao: StockDao
) : StockRepository {

    private val api: EastMoneyApi = NetworkModule.eastMoneyApi

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
            try {
                val secid = getSecid(stock.code, stock.market)
                val response = api.getStockQuote(secid)
                val data = response.data

                if (data != null) {
                    val currentPrice = (data.f43?.toDoubleOrNull() ?: 0.0) / 100.0
                    val changePercent = (data.f170?.toDoubleOrNull() ?: 0.0) / 100.0
                    val high = (data.f44?.toDoubleOrNull() ?: 0.0) / 100.0
                    val low = (data.f45?.toDoubleOrNull() ?: 0.0) / 100.0
                    val open = (data.f46?.toDoubleOrNull() ?: 0.0) / 100.0
                    val previousClose = (data.f47?.toDoubleOrNull() ?: 0.0) / 100.0

                    Result.success(
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
                } else {
                    Result.failure(Exception("无法获取股票行情"))
                }
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
