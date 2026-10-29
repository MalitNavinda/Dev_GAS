package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CylinderSizeFilter
import com.example.data.model.Distributor
import com.example.data.model.GasBrand
import com.example.data.model.QueueLevel
import com.example.data.model.QueueReport
import com.example.data.model.StockLog
import com.example.data.model.StockStatus
import com.example.data.model.UserProfile
import com.example.data.model.WatchlistItem
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.util.GeoUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class GasRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- Real-Time Observation Flows (Rule-Aligned Queries) ---

    fun observePublicDistributors(): Flow<List<Distributor>> {
        val path = "distributors"
        return db.collection(path)
            .whereEqualTo("visibility", "PUBLIC")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Distributor::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception && error !is CancellationException) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeOwnedDistributors(ownerId: String): Flow<List<Distributor>> {
        val path = "distributors"
        return db.collection(path)
            .whereEqualTo("ownerId", ownerId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Distributor::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception && error !is CancellationException) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeStationReports(stationId: String): Flow<List<QueueReport>> {
        val path = "queueReports"
        return db.collection(path)
            .whereEqualTo("stationId", stationId)
            .whereEqualTo("moderationState", "ACTIVE")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    QueueReport::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception && error !is CancellationException) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeFlaggedReportsForAdmin(): Flow<List<QueueReport>> {
        val path = "queueReports"
        return db.collection(path)
            .whereEqualTo("moderationState", "FLAGGED")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    QueueReport::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception && error !is CancellationException) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeStationStockLogs(stationId: String): Flow<List<StockLog>> {
        val path = "stockLogs"
        return db.collection(path)
            .whereEqualTo("stationId", stationId)
            .whereEqualTo("visibility", "PUBLIC")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    StockLog::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception && error !is CancellationException) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeWatchlist(userId: String): Flow<List<WatchlistItem>> {
        val path = "users/$userId/watchlist"
        return db.collection("users").document(userId).collection("watchlist")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    WatchlistItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception && error !is CancellationException) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    // --- User Profile & Admin Verification ---

    suspend fun getUserProfile(userId: String): Result<UserProfile?> = runCatching {
        val docRef = db.collection("users").document(userId)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun upsertUserProfile(displayName: String, preferredLanguage: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val safeName = displayName.trim().ifBlank { "Citizen" }.take(80)
        val safeLang = preferredLanguage.uppercase().let {
            if (it == "EN" || it == "SI" || it == "TA") it else "EN"
        }
        val docRef = db.collection("users").document(uid)
        try {
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val createPayload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "preferredLanguage" to safeLang,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            } else {
                val updatePayload = mapOf(
                    "displayName" to safeName,
                    "preferredLanguage" to safeLang,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun checkIsAdmin(userId: String, userEmail: String?, isEmailVerified: Boolean): Boolean {
        if (userEmail.equals("malith.mudunkotuwa@gmail.com", ignoreCase = true) && isEmailVerified) {
            return true
        }
        val docRef = db.collection("admins").document(userId)
        return try {
            docRef.get().await().exists()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            false
        }
    }

    // --- Distributor / Seller Operations ---

    suspend fun registerDistributor(
        name: String,
        ownerName: String,
        brand: GasBrand,
        district: String,
        town: String,
        address: String,
        phone: String,
        latitude: Double,
        longitude: Double,
        initialStock12_5kg: Int = 0,
        initialStock5kg: Int = 0,
        initialStock2_3kg: Int = 0,
        status: StockStatus = StockStatus.SOLD_OUT,
        nextArrivalNote: String = "",
        isVerifiedSeed: Boolean = false
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val stationId = "st_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("distributors").document(stationId)

        val s12 = initialStock12_5kg.coerceIn(0, 10000)
        val s5 = initialStock5kg.coerceIn(0, 10000)
        val s2 = initialStock2_3kg.coerceIn(0, 10000)
        val total = s12 + s5 + s2
        val normalizedStatus = when {
            status == StockStatus.AVAILABLE && total == 0 -> StockStatus.SOLD_OUT
            status == StockStatus.SOLD_OUT && total > 0 -> StockStatus.AVAILABLE
            else -> status
        }

        val payload = mapOf(
            "ownerId" to uid,
            "ownerName" to ownerName.trim().ifBlank { "Authorized Dealer" }.take(80),
            "name" to name.trim().take(100),
            "brand" to brand.id,
            "district" to district.trim().take(40),
            "town" to town.trim().take(60),
            "address" to address.trim().take(160),
            "phone" to phone.trim().ifBlank { "+94112000000" }.take(20),
            "latitude" to latitude.coerceIn(-90.0, 90.0),
            "longitude" to longitude.coerceIn(-180.0, 180.0),
            "geohash" to GeoUtils.encodeGeohash(latitude, longitude, 6),
            "visibility" to "PUBLIC",
            "isVerified" to isVerifiedSeed,
            "status" to normalizedStatus.id,
            "stock12_5kg" to s12,
            "stock5kg" to s5,
            "stock2_3kg" to s2,
            "nextArrivalNote" to nextArrivalNote.trim().take(120),
            "queueLevel" to QueueLevel.NONE.id,
            "estimatedQueueCount" to 0,
            "confirmationsCount" to 0,
            "discrepancyCount" to 0,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            stationId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateSellerStock(
        station: Distributor,
        newStatus: StockStatus,
        stock12_5kg: Int,
        stock5kg: Int,
        stock2_3kg: Int,
        nextArrivalNote: String,
        queueLevel: QueueLevel,
        estimatedQueueCount: Int
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(station.id.isNotBlank()) { "Station ID must not be blank" }

        val s12 = stock12_5kg.coerceIn(0, 10000)
        val s5 = stock5kg.coerceIn(0, 10000)
        val s2 = stock2_3kg.coerceIn(0, 10000)
        val total = s12 + s5 + s2

        // Deterministic Stock State Machine transition
        val resolvedStatus = when {
            newStatus == StockStatus.AVAILABLE && total == 0 -> StockStatus.SOLD_OUT
            newStatus == StockStatus.SOLD_OUT -> StockStatus.SOLD_OUT
            else -> newStatus
        }
        val finalS12 = if (resolvedStatus == StockStatus.SOLD_OUT) 0 else s12
        val finalS5 = if (resolvedStatus == StockStatus.SOLD_OUT) 0 else s5
        val finalS2 = if (resolvedStatus == StockStatus.SOLD_OUT) 0 else s2
        val finalQueueCount = if (resolvedStatus == StockStatus.SOLD_OUT) 0 else estimatedQueueCount.coerceIn(0, 1000)
        val finalQueueLevel = if (resolvedStatus == StockStatus.SOLD_OUT) QueueLevel.NONE else queueLevel

        val stationRef = db.collection("distributors").document(station.id)
        val logId = "log_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val logRef = db.collection("stockLogs").document(logId)

        val updateMap = mapOf(
            "status" to resolvedStatus.id,
            "stock12_5kg" to finalS12,
            "stock5kg" to finalS5,
            "stock2_3kg" to finalS2,
            "nextArrivalNote" to nextArrivalNote.trim().take(120),
            "queueLevel" to finalQueueLevel.id,
            "estimatedQueueCount" to finalQueueCount,
            "discrepancyCount" to 0, // Fresh seller update clears stale discrepancy flags
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val logMap = mapOf(
            "stationId" to station.id,
            "actorId" to uid,
            "stationName" to station.name.take(100),
            "brand" to station.brandEnum.id,
            "previousStatus" to station.statusEnum.id,
            "newStatus" to resolvedStatus.id,
            "stock12_5kg" to finalS12,
            "stock5kg" to finalS5,
            "stock2_3kg" to finalS2,
            "note" to nextArrivalNote.trim().ifBlank { "Dealer stock update" }.take(160),
            "visibility" to "PUBLIC",
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            stationRef.update(updateMap).await()
            logRef.set(logMap).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, stationRef.path)
            throw e
        }
    }

    suspend fun markStationSoldOut(station: Distributor): Result<Unit> =
        updateSellerStock(
            station = station,
            newStatus = StockStatus.SOLD_OUT,
            stock12_5kg = 0,
            stock5kg = 0,
            stock2_3kg = 0,
            nextArrivalNote = "Sold out — awaiting next dispatch",
            queueLevel = QueueLevel.NONE,
            estimatedQueueCount = 0
        )

    // --- Community Crowd Confirmation & Queue Reporting ---

    suspend fun submitCrowdQueueReport(
        station: Distributor,
        reporterAlias: String,
        reportedStatus: String,
        queueLengthEstimate: Int,
        comment: String,
        userLatitude: Double?,
        userLongitude: Double?
    ): Result<String> = runCatching {
        val uid = requireUserId()
        require(station.id.isNotBlank()) { "Station ID must not be blank" }

        val cleanStatus = when (reportedStatus.uppercase()) {
            "AVAILABLE", "LOW_STOCK", "SOLD_OUT" -> reportedStatus.uppercase()
            else -> "AVAILABLE"
        }
        val cleanQueueCount = queueLengthEstimate.coerceIn(0, 500)
        val computedQueueLevel = QueueLevel.fromCount(cleanQueueCount)
        val isProximity = GeoUtils.isWithinProximity(
            userLat = userLatitude,
            userLon = userLongitude,
            stationLat = station.latitude,
            stationLon = station.longitude,
            thresholdKm = 1.5
        )
        val trustWeight = if (isProximity) 2 else 1

        val reportId = "rep_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val reportRef = db.collection("queueReports").document(reportId)
        val stationRef = db.collection("distributors").document(station.id)

        val reportPayload = mapOf(
            "stationId" to station.id,
            "stationName" to station.name.take(100),
            "userId" to uid,
            "reporterAlias" to reporterAlias.trim().ifBlank { "Queue Member" }.take(60),
            "reportedStatus" to cleanStatus,
            "queueLevel" to computedQueueLevel.id,
            "queueLengthEstimate" to cleanQueueCount,
            "isProximityVerified" to isProximity,
            "weight" to trustWeight,
            "moderationState" to "ACTIVE",
            "comment" to comment.trim().take(160),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val isDiscrepancy = (station.statusEnum == StockStatus.AVAILABLE && cleanStatus == "SOLD_OUT") ||
            (station.statusEnum == StockStatus.SOLD_OUT && cleanStatus == "AVAILABLE")

        val newConfirmations = if (!isDiscrepancy) {
            station.confirmationsCount + trustWeight
        } else {
            station.confirmationsCount
        }
        val newDiscrepancies = if (isDiscrepancy) {
            station.discrepancyCount + trustWeight
        } else {
            station.discrepancyCount
        }
        val blendedQueue = if (station.estimatedQueueCount == 0L) {
            cleanQueueCount
        } else {
            ((station.estimatedQueueCount.toInt() + cleanQueueCount * trustWeight) / (1 + trustWeight))
                .coerceIn(0, 1000)
        }

        val stationCrowdUpdate = mapOf(
            "queueLevel" to QueueLevel.fromCount(blendedQueue).id,
            "estimatedQueueCount" to blendedQueue,
            "confirmationsCount" to newConfirmations,
            "discrepancyCount" to newDiscrepancies,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            reportRef.set(reportPayload).await()
            stationRef.update(stationCrowdUpdate).await()
            reportId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, reportRef.path)
            throw e
        }
    }

    suspend fun flagQueueReport(reportId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("queueReports").document(reportId)
        try {
            docRef.update(
                mapOf(
                    "moderationState" to "FLAGGED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Admin Verification & Moderation ---

    suspend fun adminSetDistributorVerified(
        stationId: String,
        isVerified: Boolean,
        visibility: String = "PUBLIC"
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("distributors").document(stationId)
        try {
            docRef.update(
                mapOf(
                    "isVerified" to isVerified,
                    "visibility" to visibility,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun adminModerateReport(
        reportId: String,
        moderationState: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("queueReports").document(reportId)
        try {
            docRef.update(
                mapOf(
                    "moderationState" to moderationState,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Watchlist Subscriptions for Nearby Restock Alerts ---

    suspend fun toggleWatchlist(
        station: Distributor,
        desiredSize: CylinderSizeFilter,
        currentlyWatched: Boolean
    ): Result<Boolean> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("watchlist").document(station.id)
        try {
            if (currentlyWatched) {
                docRef.delete().await()
                false
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "stationId" to station.id,
                    "stationName" to station.name.take(100),
                    "brand" to station.brandEnum.id,
                    "desiredSize" to desiredSize.id,
                    "lastKnownStatus" to station.statusEnum.id,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun updateWatchlistLastStatus(
        stationId: String,
        newStatus: StockStatus
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("watchlist").document(stationId)
        try {
            docRef.update(
                mapOf(
                    "lastKnownStatus" to newStatus.id,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Initial Sri Lanka Outlet Seeder (Only runs when live DB is empty) ---

    suspend fun seedSriLankaOutletsIfEmpty(isAdminUser: Boolean): Result<Int> = runCatching {
        val uid = requireUserId()
        val checkSnap = try {
            db.collection("distributors")
                .whereEqualTo("visibility", "PUBLIC")
                .limit(1)
                .get()
                .await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "distributors")
            throw e
        }
        if (!checkSnap.isEmpty) return@runCatching 0

        data class SeedOutlet(
            val id: String,
            val name: String,
            val ownerName: String,
            val brand: GasBrand,
            val district: String,
            val town: String,
            val address: String,
            val phone: String,
            val lat: Double,
            val lon: Double,
            val status: StockStatus,
            val s12: Int,
            val s5: Int,
            val s2: Int,
            val note: String,
            val queueLevel: QueueLevel,
            val queueCount: Int,
            val confirmations: Int
        )

        val seeds = listOf(
            SeedOutlet(
                "st_cmb_dehiwala_01", "Perera Gas Center - Dehiwala", "Kamal Perera",
                GasBrand.LITRO, "Colombo", "Dehiwala", "142 Galle Road, Dehiwala", "+94112714520",
                6.8511, 79.8654, StockStatus.AVAILABLE, 64, 28, 15,
                "Morning lorry unloaded at 08:15 AM", QueueLevel.MODERATE, 22, 14
            ),
            SeedOutlet(
                "st_cmb_nugegoda_02", "Wijesinghe Distributors - Nugegoda", "Ruwan Wijesinghe",
                GasBrand.LAUGFS, "Colombo", "Nugegoda", "88 High Level Road, Nugegoda", "+94112823390",
                6.8649, 79.8997, StockStatus.AVAILABLE, 32, 14, 9,
                "Yellow cylinder refill batch active", QueueLevel.SHORT, 10, 9
            ),
            SeedOutlet(
                "st_cmb_borella_03", "City Fuel & LPG Depot - Borella", "M. Farook",
                GasBrand.LITRO, "Colombo", "Borella", "210 Baseline Road, Colombo 08", "+94112698811",
                6.9147, 79.8778, StockStatus.IN_TRANSIT, 120, 40, 25,
                "Kerawalapitiya bowser lorry arriving ~4:30 PM", QueueLevel.MODERATE, 35, 11
            ),
            SeedOutlet(
                "st_gmp_negombo_04", "Lagoon Hardware & Gas - Negombo", "Jude Fernando",
                GasBrand.LITRO, "Gampaha", "Negombo", "45 Colombo Road, Negombo", "+94312224480",
                7.2008, 79.8358, StockStatus.AVAILABLE, 50, 18, 0,
                "12.5kg and 5kg in stock now", QueueLevel.SHORT, 8, 7
            ),
            SeedOutlet(
                "st_kdy_peradeniya_05", "Hill Country LPG Mart - Kandy", "bandara Senanayake",
                GasBrand.LAUGFS, "Kandy", "Peradeniya", "312 Peradeniya Road, Kandy", "+94812234100",
                7.2711, 80.6085, StockStatus.SCHEDULED, 80, 30, 20,
                "Scheduled dispatch tomorrow 09:00 AM", QueueLevel.NONE, 0, 5
            ),
            SeedOutlet(
                "st_gal_karapitiya_06", "Southern Flame Enterprises - Galle", "Chathura Silva",
                GasBrand.LITRO, "Galle", "Karapitiya", "67 Labuduwa Road, Galle", "+94912245670",
                6.0585, 80.2255, StockStatus.AVAILABLE, 42, 12, 10,
                "Fresh batch open — 1 cylinder per household", QueueLevel.MODERATE, 19, 12
            ),
            SeedOutlet(
                "st_jfn_hospital_07", "Northern Star Gas Agency - Jaffna", "S. Thayaparan",
                GasBrand.LITRO, "Jaffna", "Jaffna Town", "118 Hospital Road, Jaffna", "+94212223890",
                9.6648, 80.0162, StockStatus.SOLD_OUT, 0, 0, 0,
                "Morning batch sold out — next truck Friday", QueueLevel.NONE, 0, 16
            ),
            SeedOutlet(
                "st_kgl_town_08", "Wayamba Agro & LPG - Kurunegala", "Nimal Herath",
                GasBrand.LAUGFS, "Kurunegala", "Kurunegala", "54 Dambulla Road, Kurunegala", "+94372226190",
                7.4892, 80.3652, StockStatus.IN_TRANSIT, 60, 25, 15,
                "Mabima terminal truck en route (~1 hr)", QueueLevel.SHORT, 12, 6
            )
        )

        var created = 0
        for (seed in seeds) {
            val docRef = db.collection("distributors").document(seed.id)
            val payload = mapOf(
                "ownerId" to uid,
                "ownerName" to seed.ownerName,
                "name" to seed.name,
                "brand" to seed.brand.id,
                "district" to seed.district,
                "town" to seed.town,
                "address" to seed.address,
                "phone" to seed.phone,
                "latitude" to seed.lat,
                "longitude" to seed.lon,
                "geohash" to GeoUtils.encodeGeohash(seed.lat, seed.lon, 6),
                "visibility" to "PUBLIC",
                "isVerified" to isAdminUser,
                "status" to seed.status.id,
                "stock12_5kg" to seed.s12,
                "stock5kg" to seed.s5,
                "stock2_3kg" to seed.s2,
                "nextArrivalNote" to seed.note,
                "queueLevel" to seed.queueLevel.id,
                "estimatedQueueCount" to seed.queueCount,
                "confirmationsCount" to seed.confirmations,
                "discrepancyCount" to 0,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                docRef.set(payload).await()
                created++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, docRef.path)
            }
        }
        created
    }
}
