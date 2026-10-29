package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

data class DeviceCoordinates(
    val latitude: Double,
    val longitude: Double,
    val isInsideSriLanka: Boolean
)

object LocationHelper {
    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentCoordinates(context: Context): DeviceCoordinates? {
        if (!hasLocationPermission(context)) return null
        val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)
        return try {
            val tokenSource = CancellationTokenSource()
            val loc = client.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                tokenSource.token
            ).await() ?: client.lastLocation.await()

            if (loc != null) {
                val insideSl = loc.latitude in 5.7..10.1 && loc.longitude in 79.4..82.1
                DeviceCoordinates(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    isInsideSriLanka = insideSl
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
