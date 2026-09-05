package com.alingrin.cryptoprice3

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlin.math.abs

class PriceNotificationService(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences(
        SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE
    )

    init {
        createNotificationChannel()
    }

    fun checkPriceChange(coin: String, currentPrice: Double, lastPrice: Double) {
        if (lastPrice == 0.0) {
            Log.d(TAG, "$coin: First price update, skipping notification. Current price: $currentPrice")
            return
        }
        val threshold = SettingsActivity.getNotificationThreshold(sharedPreferences)
        val percentageChange = abs((currentPrice - lastPrice) / lastPrice * 100)
        if (percentageChange >= threshold) {
            val direction = if (currentPrice > lastPrice) "increased" else "decreased"
            showNotification(coin, String.format("%s price has %s by %.2f%%", coin, direction, percentageChange))
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Price Alerts", NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = "Notifications for significant price changes"
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun showNotification(coin: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${coin.uppercase()} Price Alert")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context).notify(
            if (coin == "bitcoin") BITCOIN_NOTIFICATION_ID else ETHEREUM_NOTIFICATION_ID,
            notification
        )
    }

    companion object {
        private const val TAG = "PriceNotification"
        private const val CHANNEL_ID = "price_alerts"
        private const val BITCOIN_NOTIFICATION_ID = 1
        private const val ETHEREUM_NOTIFICATION_ID = 2
    }
}