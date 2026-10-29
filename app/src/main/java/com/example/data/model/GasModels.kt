package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AppLanguage(val code: String, val nativeLabel: String) {
    EN("EN", "EN"),
    SI("SI", "සිං"),
    TA("TA", "தமிழ்");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: EN
    }
}

enum class GasBrand(val id: String, val displayName: String) {
    LITRO("LITRO", "Litro Gas"),
    LAUGFS("LAUGFS", "LAUGFS Gas");

    companion object {
        fun fromId(id: String?): GasBrand =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: LITRO
    }
}

enum class CylinderSizeFilter(val id: String, val label: String) {
    ALL("ANY", "All Sizes"),
    SIZE_12_5KG("12_5KG", "12.5 kg"),
    SIZE_5KG("5KG", "5.0 kg"),
    SIZE_2_3KG("2_3KG", "2.3 kg")
}

enum class StockStatus(val id: String) {
    SCHEDULED("SCHEDULED"),
    IN_TRANSIT("IN_TRANSIT"),
    AVAILABLE("AVAILABLE"),
    SOLD_OUT("SOLD_OUT");

    companion object {
        fun fromId(id: String?): StockStatus =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SOLD_OUT
    }
}

enum class QueueLevel(val id: String) {
    NONE("NONE"),
    SHORT("SHORT"),
    MODERATE("MODERATE"),
    LONG("LONG");

    companion object {
        fun fromId(id: String?): QueueLevel =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: NONE

        fun fromCount(count: Int): QueueLevel = when {
            count <= 0 -> NONE
            count < 15 -> SHORT
            count <= 50 -> MODERATE
            else -> LONG
        }
    }
}

data class SriLankaDistrict(
    val name: String,
    val sinhalaName: String,
    val tamilName: String,
    val centerLat: Double,
    val centerLon: Double
)

object SriLankaDistricts {
    // Default Colombo Fort / Town Hall reference when GPS permission is not granted yet
    const val DEFAULT_COLOMBO_LAT = 6.9271
    const val DEFAULT_COLOMBO_LON = 79.8612

    val all: List<SriLankaDistrict> = listOf(
        SriLankaDistrict("Colombo", "කොළඹ", "கொழும்பு", 6.9271, 79.8612),
        SriLankaDistrict("Gampaha", "ගම්පහ", "கம்பஹா", 7.0840, 80.0098),
        SriLankaDistrict("Kalutara", "කළුතර", "களுத்துறை", 6.5854, 79.9607),
        SriLankaDistrict("Kandy", "මහනුවර", "கண்டி", 7.2906, 80.6337),
        SriLankaDistrict("Galle", "ගාල්ල", "காலி", 6.0535, 80.2210),
        SriLankaDistrict("Matara", "මාතර", "மாத்தறை", 5.9549, 80.5550),
        SriLankaDistrict("Kurunegala", "කුරුණෑගල", "குருநாகல்", 7.4863, 80.3647),
        SriLankaDistrict("Jaffna", "යාපනය", "யாழ்ப்பாணம்", 9.6615, 80.0255),
        SriLankaDistrict("Anuradhapura", "අනුරාධපුර", "அனுராதபுரம்", 8.3114, 80.4037),
        SriLankaDistrict("Ratnapura", "රත්නපුර", "இரத்தினபுரி", 6.6828, 80.3992),
        SriLankaDistrict("Badulla", "බදුල්ල", "பதுளை", 6.9934, 81.0550),
        SriLankaDistrict("Batticaloa", "මඩකලපුව", "மட்டக்களப்பு", 7.7310, 81.6747),
        SriLankaDistrict("Trincomalee", "ත්‍රිකුණාමලය", "திருகோணமலை", 8.5874, 81.2152),
        SriLankaDistrict("Nuwara Eliya", "නුවරඑළිය", "நுவரெலியா", 6.9497, 80.7891),
        SriLankaDistrict("Puttalam", "පුත්තලම", "புத்தளம்", 8.0362, 79.8283),
        SriLankaDistrict("Hambantota", "හම්බන්තොට", "அம்பாந்தோட்டை", 6.1429, 81.1212)
    )

    fun findByName(name: String): SriLankaDistrict =
        all.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: all.first()
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val preferredLanguage: String = "EN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Distributor(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val name: String = "",
    val brand: String = "LITRO",
    val district: String = "Colombo",
    val town: String = "",
    val address: String = "",
    val phone: String = "",
    val latitude: Double = 6.9271,
    val longitude: Double = 79.8612,
    val geohash: String = "tc0z9x",
    val visibility: String = "PUBLIC",
    val isVerified: Boolean = false,
    val status: String = "SOLD_OUT",
    val stock12_5kg: Long = 0L,
    val stock5kg: Long = 0L,
    val stock2_3kg: Long = 0L,
    val nextArrivalNote: String = "",
    val queueLevel: String = "NONE",
    val estimatedQueueCount: Long = 0L,
    val confirmationsCount: Long = 0L,
    val discrepancyCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val totalCylinders: Long
        get() = stock12_5kg + stock5kg + stock2_3kg

    val brandEnum: GasBrand
        get() = GasBrand.fromId(brand)

    val statusEnum: StockStatus
        get() = StockStatus.fromId(status)

    val queueLevelEnum: QueueLevel
        get() = QueueLevel.fromId(queueLevel)

    val estimatedWaitMinutes: Int
        get() = (estimatedQueueCount.toInt() * 1.4).toInt().coerceAtLeast(0)

    val trustConfidencePercent: Int
        get() {
            val totalVotes = confirmationsCount + discrepancyCount
            if (totalVotes <= 0L) return if (isVerified) 92 else 75
            val ratio = confirmationsCount.toDouble() / totalVotes.toDouble()
            val base = (ratio * 100.0).toInt()
            return if (isVerified) base.coerceIn(35, 99) else base.coerceIn(20, 95)
        }

    val hasCrowdDiscrepancy: Boolean
        get() = statusEnum == StockStatus.AVAILABLE && discrepancyCount >= 3L && discrepancyCount > confirmationsCount

    fun matchesCylinderFilter(filter: CylinderSizeFilter): Boolean = when (filter) {
        CylinderSizeFilter.ALL -> true
        CylinderSizeFilter.SIZE_12_5KG -> stock12_5kg > 0L
        CylinderSizeFilter.SIZE_5KG -> stock5kg > 0L
        CylinderSizeFilter.SIZE_2_3KG -> stock2_3kg > 0L
    }
}

data class DistributorWithDistance(
    val distributor: Distributor,
    val distanceKm: Double
)

@IgnoreExtraProperties
data class StockLog(
    @DocumentId val id: String = "",
    val stationId: String = "",
    val actorId: String = "",
    val stationName: String = "",
    val brand: String = "LITRO",
    val previousStatus: String = "SOLD_OUT",
    val newStatus: String = "AVAILABLE",
    val stock12_5kg: Long = 0L,
    val stock5kg: Long = 0L,
    val stock2_3kg: Long = 0L,
    val note: String = "",
    val visibility: String = "PUBLIC",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class QueueReport(
    @DocumentId val id: String = "",
    val stationId: String = "",
    val stationName: String = "",
    val userId: String = "",
    val reporterAlias: String = "",
    val reportedStatus: String = "AVAILABLE",
    val queueLevel: String = "SHORT",
    val queueLengthEstimate: Long = 0L,
    val isProximityVerified: Boolean = false,
    val weight: Long = 1L,
    val moderationState: String = "ACTIVE",
    val comment: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WatchlistItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val stationId: String = "",
    val stationName: String = "",
    val brand: String = "LITRO",
    val desiredSize: String = "ANY",
    val lastKnownStatus: String = "SOLD_OUT",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class InAppAlert(
    val id: String,
    val stationId: String,
    val stationName: String,
    val brand: GasBrand,
    val newStatus: StockStatus,
    val message: String,
    val timestampMillis: Long
)
