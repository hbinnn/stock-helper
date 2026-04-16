package com.example.stockhelper.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.stockhelper.MainActivity
import com.example.stockhelper.R
import com.example.stockhelper.data.local.AppDatabase
import com.example.stockhelper.data.repository.StockRepository
import com.example.stockhelper.data.repository.StockRepositoryImpl
import com.example.stockhelper.domain.model.AlertState
import com.example.stockhelper.domain.model.Stock
import com.example.stockhelper.domain.model.StockQuote
import com.example.stockhelper.domain.model.TradeRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PriceMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: StockRepository
    private lateinit var notificationHelper: NotificationHelper

    private var refreshJob: Job? = null
    private var isRunning = false

    companion object {
        const val ACTION_START = "com.example.stockhelper.action.START_MONITORING"
        const val ACTION_STOP = "com.example.stockhelper.action.STOP_MONITORING"
        const val FOREGROUND_NOTIFICATION_ID = 1002
        const val CHANNEL_ID_SERVICE = "stock_monitor_service"
    }

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(applicationContext)
        repository = StockRepositoryImpl(database.stockDao())
        notificationHelper = NotificationHelper(applicationContext)
        createServiceChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startMonitoring()
            ACTION_STOP -> stopMonitoring()
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        if (isRunning) return
        isRunning = true

        val notification = createForegroundNotification()
        startForeground(FOREGROUND_NOTIFICATION_ID, notification)

        refreshJob = serviceScope.launch {
            while (isRunning) {
                loadStocksAndCheckAlerts()
                delay(15000) // 15 seconds
            }
        }
    }

    private fun stopMonitoring() {
        isRunning = false
        refreshJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun loadStocksAndCheckAlerts() {
        val stocks = mutableListOf<Stock>()
        repository.getAllStocks().collect { list ->
            stocks.clear()
            stocks.addAll(list)
        }

        if (stocks.isEmpty()) return

        val quotes = repository.refreshQuotes(stocks)
        val quotesMap = mutableMapOf<String, StockQuote>()
        quotes.forEach { (code, result) ->
            result.onSuccess { quote -> quotesMap[code] = quote }
        }

        stocks.forEach { stock ->
            val quote = quotesMap[stock.code] ?: return@forEach
            checkAndSendAlert(stock, quote)
        }
    }

    private suspend fun checkAndSendAlert(stock: Stock, quote: StockQuote) {
        val currentPrice = quote.currentPrice
        val isBuyFirst = stock.tradeType == "BUY_FIRST"

        val database = AppDatabase.getDatabase(applicationContext)
        var alertState = database.alertStateDao().getAlertState(stock.code)
        if (alertState == null) {
            alertState = AlertState(stockCode = stock.code)
            database.alertStateDao().insertOrUpdate(alertState)
        }

        val buyReached = currentPrice <= stock.targetBuyPrice
        val sellReached = currentPrice >= stock.targetSellPrice
        val tShares = getTShares(stock)

        if (isBuyFirst) {
            // 先买后卖模式
            if (buyReached && !alertState.firstConditionReached) {
                alertState = alertState.copy(
                    firstConditionReached = true,
                    firstConditionReachedAt = System.currentTimeMillis()
                )
                database.alertStateDao().insertOrUpdate(alertState)
                // 记录历史
                database.tradeRecordDao().insert(
                    TradeRecord(
                        stockCode = stock.code,
                        stockName = stock.name,
                        recordType = "BUY_ALERT",
                        currentPrice = currentPrice,
                        targetPrice = stock.targetBuyPrice,
                        tradeType = stock.tradeType,
                        tShares = tShares
                    )
                )
                notificationHelper.sendPriceAlert(
                    stockCode = stock.code,
                    stockName = stock.name,
                    message = "【买入提醒】${stock.name}(${stock.code}) 当前价格 ${String.format("%.2f", currentPrice)} 元，已达到目标买入价 ${String.format("%.2f", stock.targetBuyPrice)} 元",
                    isBuy = true
                )
            }

            if (sellReached && alertState.firstConditionReached && !alertState.sellAlertSent) {
                alertState = alertState.copy(sellAlertSent = true)
                database.alertStateDao().insertOrUpdate(alertState)
                // 记录历史
                database.tradeRecordDao().insert(
                    TradeRecord(
                        stockCode = stock.code,
                        stockName = stock.name,
                        recordType = "SELL_ALERT",
                        currentPrice = currentPrice,
                        targetPrice = stock.targetSellPrice,
                        tradeType = stock.tradeType,
                        tShares = tShares
                    )
                )
                val expectedProfit = (stock.targetSellPrice - stock.targetBuyPrice) * tShares * 0.999
                notificationHelper.sendPriceAlert(
                    stockCode = stock.code,
                    stockName = stock.name,
                    message = "【卖出提醒】${stock.name}(${stock.code}) 当前价格 ${String.format("%.2f", currentPrice)} 元，已达到目标卖出价 ${String.format("%.2f", stock.targetSellPrice)} 元，预估收益 ${String.format("%.2f", expectedProfit)} 元",
                    isBuy = false
                )
            }
        } else {
            // 先卖后买模式
            if (sellReached && !alertState.firstConditionReached) {
                alertState = alertState.copy(
                    firstConditionReached = true,
                    firstConditionReachedAt = System.currentTimeMillis()
                )
                database.alertStateDao().insertOrUpdate(alertState)
                // 记录历史
                database.tradeRecordDao().insert(
                    TradeRecord(
                        stockCode = stock.code,
                        stockName = stock.name,
                        recordType = "SELL_ALERT",
                        currentPrice = currentPrice,
                        targetPrice = stock.targetSellPrice,
                        tradeType = stock.tradeType,
                        tShares = tShares
                    )
                )
                val expectedProfit = (stock.targetSellPrice - stock.targetBuyPrice) * tShares * 0.999
                notificationHelper.sendPriceAlert(
                    stockCode = stock.code,
                    stockName = stock.name,
                    message = "【卖出提醒】${stock.name}(${stock.code}) 当前价格 ${String.format("%.2f", currentPrice)} 元，已达到目标卖出价 ${String.format("%.2f", stock.targetSellPrice)} 元，预估收益 ${String.format("%.2f", expectedProfit)} 元",
                    isBuy = false
                )
            }

            if (buyReached && alertState.firstConditionReached && !alertState.buyAlertSent) {
                alertState = alertState.copy(buyAlertSent = true)
                database.alertStateDao().insertOrUpdate(alertState)
                // 记录历史
                database.tradeRecordDao().insert(
                    TradeRecord(
                        stockCode = stock.code,
                        stockName = stock.name,
                        recordType = "BUY_ALERT",
                        currentPrice = currentPrice,
                        targetPrice = stock.targetBuyPrice,
                        tradeType = stock.tradeType,
                        tShares = tShares
                    )
                )
                notificationHelper.sendPriceAlert(
                    stockCode = stock.code,
                    stockName = stock.name,
                    message = "【买入提醒】${stock.name}(${stock.code}) 当前价格 ${String.format("%.2f", currentPrice)} 元，已达到目标买入价 ${String.format("%.2f", stock.targetBuyPrice)} 元",
                    isBuy = true
                )
            }
        }
    }

    private fun getTShares(stock: Stock): Int {
        return if (stock.tTradeType == "PERCENT") {
            (stock.shares * stock.tSharesPercent / 100)
        } else {
            stock.tShares
        }
    }

    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID_SERVICE)
            .setContentTitle("大富翁助手")
            .setContentText("正在监控股票价格...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createServiceChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_SERVICE,
                "价格监控服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "后台价格监控通知" }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        refreshJob?.cancel()
        serviceScope.cancel()
    }
}
