package com.example.stockhelper.data.repository

import com.example.stockhelper.domain.model.Stock
import com.example.stockhelper.domain.model.StockQuote
import kotlinx.coroutines.flow.Flow

interface StockRepository {
    fun getAllStocks(): Flow<List<Stock>>
    suspend fun getStockByCode(code: String): Stock?
    suspend fun addStock(stock: Stock)
    suspend fun deleteStock(code: String)
    suspend fun getStockQuote(stock: Stock): Result<StockQuote>
    suspend fun refreshQuotes(stocks: List<Stock>): Map<String, Result<StockQuote>>
}
