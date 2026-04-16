package com.example.stockhelper.util

import android.content.Context
import android.content.Intent
import android.os.Build

object ServiceManager {
    fun startPriceMonitorService(context: Context) {
        val intent = Intent(context, PriceMonitorService::class.java).apply {
            action = PriceMonitorService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopPriceMonitorService(context: Context) {
        val intent = Intent(context, PriceMonitorService::class.java).apply {
            action = PriceMonitorService.ACTION_STOP
        }
        context.startService(intent)
    }
}
