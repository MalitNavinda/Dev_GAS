package com.example.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {
    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"
    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Encodes geographic coordinates into a standard Base32 Geohash string for fast prefix indexing.
     */
    fun encodeGeohash(latitude: Double, longitude: Double, precision: Int = 6): String {
        val clampedLat = latitude.coerceIn(-90.0, 90.0)
        val clampedLon = longitude.coerceIn(-180.0, 180.0)
        var minLat = -90.0
        var maxLat = 90.0
        var minLon = -180.0
        var maxLon = 180.0

        val hash = StringBuilder()
        var isEven = true
        var bit = 0
        var ch = 0

        while (hash.length < precision.coerceIn(4, 12)) {
            if (isEven) {
                val mid = (minLon + maxLon) / 2.0
                if (clampedLon >= mid) {
                    ch = ch or (1 shl (4 - bit))
                    minLon = mid
                } else {
                    maxLon = mid
                }
            } else {
                val mid = (minLat + maxLat) / 2.0
                if (clampedLat >= mid) {
                    ch = ch or (1 shl (4 - bit))
                    minLat = mid
                } else {
                    maxLat = mid
                }
            }
            isEven = !isEven
            if (bit < 4) {
                bit++
            } else {
                hash.append(BASE32[ch])
                bit = 0
                ch = 0
            }
        }
        return hash.toString()
    }

    /**
     * Computes great-circle distance in kilometers using the Haversine formula.
     */
    fun haversineDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2.0).pow(2.0) +
            cos(rLat1) * cos(rLat2) * sin(dLon / 2.0).pow(2.0)
        val c = 2.0 * atan2(sqrt(a), sqrt((1.0 - a).coerceAtLeast(0.0)))
        return EARTH_RADIUS_KM * c
    }

    fun formatDistanceKm(distanceKm: Double?): String {
        if (distanceKm == null || distanceKm.isNaN()) return "-- km"
        return if (distanceKm < 1.0) {
            val meters = (distanceKm * 1000.0).roundToInt().coerceAtLeast(50)
            "$meters m"
        } else {
            val tenths = (distanceKm * 10.0).roundToInt() / 10.0
            "$tenths km"
        }
    }

    fun isWithinProximity(
        userLat: Double?,
        userLon: Double?,
        stationLat: Double,
        stationLon: Double,
        thresholdKm: Double = 1.5
    ): Boolean {
        if (userLat == null || userLon == null) return false
        return haversineDistanceKm(userLat, userLon, stationLat, stationLon) <= thresholdKm
    }
}
