package com.example.stockhelper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.stockhelper.domain.model.AlertState

@Dao
interface AlertStateDao {
    @Query("SELECT * FROM alert_states WHERE stockCode = :code")
    suspend fun getAlertState(code: String): AlertState?

    @Query("SELECT * FROM alert_states")
    suspend fun getAllAlertStates(): List<AlertState>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(state: AlertState)

    @Query("DELETE FROM alert_states WHERE stockCode = :code")
    suspend fun deleteByCode(code: String)

    @Query("DELETE FROM alert_states")
    suspend fun deleteAll()

    @Query("""
        UPDATE alert_states SET
            buyAlertSent = 0,
            sellAlertSent = 0,
            firstConditionReached = 0,
            firstConditionReachedAt = NULL,
            lastUpdated = :timestamp
    """)
    suspend fun resetAllStates(timestamp: Long = System.currentTimeMillis())
}
