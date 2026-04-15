package com.example.stockhelper.ui.screens.add

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockhelper.data.local.AppDatabase
import com.example.stockhelper.data.repository.StockRepository
import com.example.stockhelper.data.repository.StockRepositoryImpl
import com.example.stockhelper.domain.model.Stock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddStockViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StockRepository

    private val _uiState = MutableStateFlow(AddStockUiState())
    val uiState: StateFlow<AddStockUiState> = _uiState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = StockRepositoryImpl(database.stockDao())
    }

    /**
     * 根据股票代码自动识别市场
     * 上海: 6开头 (包括688科创板)
     * 深圳: 0或3开头
     */
    private fun detectMarket(code: String): String {
        return when {
            code.startsWith("6") -> "sh"
            code.startsWith("0") || code.startsWith("3") -> "sz"
            else -> "sh" // 默认上海
        }
    }

    fun updateCode(code: String) {
        val market = detectMarket(code)
        _uiState.update {
            it.copy(
                code = code,
                market = market,
                fetchedName = null,
                currentPrice = null,
                changePercent = null,
                costPrice = null,
                costPriceInput = "",
                targetBuyPrice = "",
                targetSellPrice = "",
                buyPriceType = "PRICE",
                sellPriceType = "PRICE",
                buyPercent = "",
                sellPercent = "",
                error = null
            )
        }
    }

    fun updateCostPrice(price: String) {
        _uiState.update { it.copy(costPriceInput = price, error = null) }
    }

    fun updateTargetBuyPriceType(type: String) {
        _uiState.update { it.copy(buyPriceType = type, error = null) }
    }

    fun updateTargetSellPriceType(type: String) {
        _uiState.update { it.copy(sellPriceType = type, error = null) }
    }

    fun updateBuyPercent(percent: String) {
        _uiState.update { it.copy(buyPercent = percent, error = null) }
    }

    fun updateSellPercent(percent: String) {
        _uiState.update { it.copy(sellPercent = percent, error = null) }
    }

    fun updateTargetBuyPrice(price: String) {
        _uiState.update { it.copy(targetBuyPrice = price, error = null) }
    }

    fun updateTargetSellPrice(price: String) {
        _uiState.update { it.copy(targetSellPrice = price, error = null) }
    }

    fun updateShares(shares: String) {
        _uiState.update { it.copy(shares = shares, error = null) }
    }

    fun updateTradeType(tradeType: String) {
        _uiState.update { it.copy(tradeType = tradeType, error = null) }
    }

    fun updateTTradeType(tTradeType: String) {
        _uiState.update { it.copy(tTradeType = tTradeType, error = null) }
    }

    fun updateTShares(tShares: String) {
        _uiState.update { it.copy(tShares = tShares, error = null) }
    }

    fun updateTSharesPercent(tSharesPercent: String) {
        _uiState.update { it.copy(tSharesPercent = tSharesPercent, error = null) }
    }

    fun fetchStockInfo() {
        val state = _uiState.value
        if (state.code.length < 6) {
            _uiState.update { it.copy(error = "请输入完整的6位股票代码") }
            return
        }

        _uiState.update { it.copy(isLoadingInfo = true, error = null) }

        viewModelScope.launch {
            val tempStock = Stock(
                code = state.code,
                name = "",
                costPrice = 0.0,
                targetBuyPrice = 0.0,
                targetSellPrice = 0.0,
                shares = 0,
                market = state.market
            )

            repository.getStockQuote(tempStock)
                .onSuccess { quote ->
                    _uiState.update {
                        it.copy(
                            fetchedName = quote.name,
                            name = quote.name,
                            currentPrice = quote.currentPrice,
                            changePercent = quote.changePercent,
                            costPrice = quote.currentPrice,  // 默认成本价为现价
                            costPriceInput = String.format("%.2f", quote.currentPrice),
                            isLoadingInfo = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            fetchedName = null,
                            currentPrice = null,
                            changePercent = null,
                            costPrice = null,
                            isLoadingInfo = false,
                            error = "无法获取股票信息，请检查代码是否正确"
                        )
                    }
                }
        }
    }

    fun saveStock() {
        val state = _uiState.value

        // 验证输入
        if (state.code.isBlank()) {
            _uiState.update { it.copy(error = "请输入股票代码") }
            return
        }
        if (state.code.length < 6) {
            _uiState.update { it.copy(error = "请输入完整的6位股票代码") }
            return
        }
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "请先获取股票信息") }
            return
        }

        // 成本价验证
        val costPrice = state.costPriceInput.toDoubleOrNull()
        if (costPrice == null || costPrice <= 0) {
            _uiState.update { it.copy(error = "请输入有效的成本价") }
            return
        }

        // 买入价计算
        val buyPrice: Double
        val buyPercentValue: Double
        if (state.buyPriceType == "PERCENT") {
            buyPercentValue = state.buyPercent.toDoubleOrNull() ?: 0.0
            if (buyPercentValue <= 0) {
                _uiState.update { it.copy(error = "请输入有效的买入百分比") }
                return
            }
            buyPrice = costPrice * (1 - buyPercentValue / 100.0)
        } else {
            buyPrice = state.targetBuyPrice.toDoubleOrNull() ?: 0.0
            if (buyPrice <= 0) {
                _uiState.update { it.copy(error = "请输入有效的目标买入价") }
                return
            }
            buyPercentValue = 0.0
        }

        // 卖出价计算
        val sellPrice: Double
        val sellPercentValue: Double
        if (state.sellPriceType == "PERCENT") {
            sellPercentValue = state.sellPercent.toDoubleOrNull() ?: 0.0
            if (sellPercentValue <= 0) {
                _uiState.update { it.copy(error = "请输入有效的卖出百分比") }
                return
            }
            sellPrice = costPrice * (1 + sellPercentValue / 100.0)
        } else {
            sellPrice = state.targetSellPrice.toDoubleOrNull() ?: 0.0
            if (sellPrice <= 0) {
                _uiState.update { it.copy(error = "请输入有效的目标卖出价") }
                return
            }
            sellPercentValue = 0.0
        }

        if (sellPrice <= buyPrice) {
            _uiState.update { it.copy(error = "目标卖出价必须大于买入价") }
            return
        }

        val shares = state.shares.toIntOrNull()
        if (shares == null || shares <= 0) {
            _uiState.update { it.copy(error = "请输入有效的持仓数量") }
            return
        }

        // 做T数量验证
        val tSharesValue: Int
        val tSharesPercentValue: Int
        if (state.tTradeType == "PERCENT") {
            tSharesPercentValue = state.tSharesPercent.toIntOrNull() ?: 0
            if (tSharesPercentValue <= 0 || tSharesPercentValue > 100) {
                _uiState.update { it.copy(error = "请输入有效的做T比例(1-100)") }
                return
            }
            tSharesValue = 0
        } else {
            tSharesValue = state.tShares.toIntOrNull() ?: 0
            if (tSharesValue <= 0) {
                _uiState.update { it.copy(error = "请输入有效的做T股数") }
                return
            }
            // 先卖后买时，卖出数量不能超过持仓
            if (state.tradeType == "SELL_FIRST" && tSharesValue > shares) {
                _uiState.update { it.copy(error = "做T股数不能超过持仓数量") }
                return
            }
            tSharesPercentValue = 0
        }

        val stock = Stock(
            code = state.code,
            name = state.name,
            costPrice = costPrice,
            targetBuyPrice = buyPrice,
            targetSellPrice = sellPrice,
            buyPriceType = state.buyPriceType,
            sellPriceType = state.sellPriceType,
            buyPercent = if (state.buyPriceType == "PERCENT") buyPercentValue else 0.0,
            sellPercent = if (state.sellPriceType == "PERCENT") sellPercentValue else 0.0,
            shares = shares,
            market = state.market,
            tradeType = state.tradeType,
            tTradeType = state.tTradeType,
            tShares = tSharesValue,
            tSharesPercent = tSharesPercentValue
        )

        viewModelScope.launch {
            try {
                repository.addStock(stock)
                _uiState.update { it.copy(isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "保存失败: ${e.message}") }
            }
        }
    }
}
