package com.example.stockhelper

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.stockhelper.util.AlertStateResetWorker
import com.example.stockhelper.util.NotificationHelper
import java.util.Calendar
import java.util.concurrent.TimeUnit

class RichHelperApp : Application() {

    lateinit var notificationHelper: NotificationHelper
        private set

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        scheduleDailyAlertReset()
    }

    private fun scheduleDailyAlertReset() {
        val initialDelay = calculateInitialDelay()

        val workRequest = PeriodicWorkRequestBuilder<AlertStateResetWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            AlertStateResetWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun calculateInitialDelay(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9) // 每天9点重置
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        return target.timeInMillis - now.timeInMillis
    }
}
