package com.example.stockhelper.data.local

import androidx.room.*
import com.example.stockhelper.domain.model.Stock
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT * FROM stocks ORDER BY code ASC")
    fun getAllStocks(): Flow<List<Stock>>

    @Query("SELECT * FROM stocks WHERE code = :code")
    suspend fun getStockByCode(code: String): Stock?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStock(stock: Stock)

    @Delete
    suspend fun deleteStock(stock: Stock)

    @Query("DELETE FROM stocks WHERE code = :code")
    suspend fun deleteStockByCode(code: String)
}
