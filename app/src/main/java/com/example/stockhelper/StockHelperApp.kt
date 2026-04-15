package com.example.stockhelper

import android.app.Application
import com.example.stockhelper.util.NotificationHelper

class RichHelperApp : Application() {

    lateinit var notificationHelper: NotificationHelper
        private set

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
    }
}
