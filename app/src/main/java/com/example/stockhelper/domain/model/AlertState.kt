package com.example.stockhelper.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alert_states")
data class AlertState(
    @PrimaryKey
    val stockCode: String,
    val buyAlertSent: Boolean = false,
    val sellAlertSent: Boolean = false,
    val firstConditionReached: Boolean = false,
    val firstConditionReachedAt: Long? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)
