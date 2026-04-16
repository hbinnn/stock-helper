package com.example.stockhelper.ui.screens.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockhelper.data.local.AppDatabase
import com.example.stockhelper.domain.model.TradeRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val records: List<TradeRecord> = emptyList(),
    val isLoading: Boolean = false,
    val filterType: String? = null // null = all, "BUY_ALERT", "SELL_ALERT"
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val tradeRecordDao = AppDatabase.getDatabase(application).tradeRecordDao()

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadRecords()
    }

    private fun loadRecords() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            tradeRecordDao.getAllRecords().collect { records ->
                val filtered = _uiState.value.filterType?.let { type ->
                    records.filter { it.recordType == type }
                } ?: records
                _uiState.update { it.copy(records = filtered, isLoading = false) }
            }
        }
    }

    fun setFilter(filterType: String?) {
        _uiState.update { it.copy(filterType = filterType) }
        loadRecords()
    }

    fun markAsHandled(id: Long) {
        viewModelScope.launch {
            tradeRecordDao.markAsHandled(id)
        }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            tradeRecordDao.delete(id)
        }
    }
}
