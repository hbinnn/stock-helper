package com.example.stockhelper.util

import android.app.Service
import android.content.Intent
import android.os.IBinder

class PriceMonitorService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}
