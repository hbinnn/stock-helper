package com.example.stockhelper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.stockhelper.domain.model.TradeRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeRecordDao {
    @Query("SELECT * FROM trade_records ORDER BY triggeredAt DESC")
    fun getAllRecords(): Flow<List<TradeRecord>>

    @Query("SELECT * FROM trade_records WHERE stockCode = :code ORDER BY triggeredAt DESC")
    fun getRecordsByStock(code: String): Flow<List<TradeRecord>>

    @Query("SELECT * FROM trade_records WHERE isHandled = 0 ORDER BY triggeredAt DESC")
    fun getUnhandledRecords(): Flow<List<TradeRecord>>

    @Query("SELECT * FROM trade_records WHERE recordType = :type ORDER BY triggeredAt DESC")
    fun getRecordsByType(type: String): Flow<List<TradeRecord>>

    @Insert
    suspend fun insert(record: TradeRecord)

    @Query("UPDATE trade_records SET isHandled = 1 WHERE id = :id")
    suspend fun markAsHandled(id: Long)

    @Query("DELETE FROM trade_records WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM trade_records WHERE triggeredAt < :timestamp")
    suspend fun deleteOldRecords(timestamp: Long)
}
