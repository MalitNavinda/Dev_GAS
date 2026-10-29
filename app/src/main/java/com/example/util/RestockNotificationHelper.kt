package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.GasBrand
import com.example.data.model.StockStatus

object RestockNotificationHelper {
    private const val CHANNEL_ID = "lankagas_restock_alerts"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_desc)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    fun postRestockNotification(
        context: Context,
        stationId: String,
        stationName: String,
        brand: GasBrand,
        newStatus: StockStatus,
        summaryMessage: String
    ) {
        ensureChannel(context)
        if (!hasNotificationPermission(context)) return

        val title = when (newStatus) {
            StockStatus.AVAILABLE -> "${brand.displayName} Available Now!"
            StockStatus.IN_TRANSIT -> "${brand.displayName} Truck En Route!"
            else -> "${brand.displayName} Update · $stationName"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("$stationName: $summaryMessage")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$stationName — $summaryMessage"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(stationId.hashCode(), notification)
        } catch (_: Exception) {
            // Safe guard if notifications are disabled at OS settings level
        }
    }
}
