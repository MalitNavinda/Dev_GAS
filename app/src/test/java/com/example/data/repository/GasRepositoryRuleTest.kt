package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CylinderSizeFilter
import com.example.data.model.GasBrand
import com.example.data.model.QueueLevel
import com.example.data.model.StockStatus
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class GasRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun registerDistributor_andObservePublic_emitsCreatedStation() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(8)
        signInTestUser("seller_$suffix@lankagas.lk")
        val repository = GasRepository(firestore, auth)

        val stationIdResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.registerDistributor(
                name = "Kandy Litro Depot $suffix",
                ownerName = "Sunil Bandara",
                brand = GasBrand.LITRO,
                district = "Kandy",
                town = "Peradeniya",
                address = "120 Peradeniya Road, Kandy",
                phone = "+94812234567",
                latitude = 7.2906,
                longitude = 80.6337,
                initialStock12_5kg = 40,
                initialStock5kg = 15,
                initialStock2_3kg = 8,
                status = StockStatus.AVAILABLE
            )
        }
        assertTrue("Expected registration success: ${stationIdResult.exceptionOrNull()}", stationIdResult.isSuccess)
        val stationId = stationIdResult.getOrThrow()

        val emittedStations = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observePublicDistributors().first { list -> list.any { it.id == stationId } }
        }
        val created = emittedStations.first { it.id == stationId }
        assertEquals(40L, created.stock12_5kg)
        assertEquals("AVAILABLE", created.status)
    }

    @Test
    fun updateSellerStock_andMarkSoldOut_transitionsStateAndAppendsAuditLog() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(8)
        signInTestUser("dealer_$suffix@lankagas.lk")
        val repository = GasRepository(firestore, auth)

        val stationId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.registerDistributor(
                name = "Galle LAUGFS Center $suffix",
                ownerName = "Nimal Silva",
                brand = GasBrand.LAUGFS,
                district = "Galle",
                town = "Karapitiya",
                address = "55 Labuduwa Rd, Galle",
                phone = "+94912233445",
                latitude = 6.0535,
                longitude = 80.2210,
                initialStock12_5kg = 25,
                initialStock5kg = 10,
                initialStock2_3kg = 5,
                status = StockStatus.AVAILABLE
            ).getOrThrow()
        }

        val initialStation = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observePublicDistributors().first { list -> list.any { it.id == stationId } }
                .first { it.id == stationId }
        }

        val soldOutResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.markStationSoldOut(initialStation)
        }
        assertTrue("Expected markStationSoldOut to succeed: ${soldOutResult.exceptionOrNull()}", soldOutResult.isSuccess)

        val updatedStation = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observePublicDistributors().first { list ->
                list.any { it.id == stationId && it.status == "SOLD_OUT" }
            }.first { it.id == stationId }
        }
        assertEquals(0L, updatedStation.totalCylinders)

        val logs = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeStationStockLogs(stationId).first { it.isNotEmpty() }
        }
        assertEquals("SOLD_OUT", logs.first().newStatus)
    }

    @Test
    fun submitCrowdQueueReport_andWatchlist_respectOwnershipAndPermissions() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(8)
        val sellerUid = signInTestUser("seller_crowd_$suffix@lankagas.lk")
        val sellerRepo = GasRepository(firestore, auth)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            sellerRepo.upsertUserProfile("Seller Kamal", "SI").getOrThrow()
        }

        val stationId = withTimeout(DEFAULT_TIMEOUT_MS) {
            sellerRepo.registerDistributor(
                name = "Colombo Gas Mart $suffix",
                ownerName = "Kamal",
                brand = GasBrand.LITRO,
                district = "Colombo",
                town = "Dehiwala",
                address = "100 Galle Rd, Dehiwala",
                phone = "+94112711111",
                latitude = 6.8511,
                longitude = 79.8654,
                initialStock12_5kg = 50,
                initialStock5kg = 20,
                initialStock2_3kg = 10,
                status = StockStatus.AVAILABLE
            ).getOrThrow()
        }

        val station = withTimeout(FLOW_TIMEOUT_MS) {
            sellerRepo.observePublicDistributors().first { list -> list.any { it.id == stationId } }
                .first { it.id == stationId }
        }

        // Switch to consumer user
        val consumerUid = signInTestUser("consumer_crowd_$suffix@lankagas.lk")
        val consumerRepo = GasRepository(firestore, auth)

        // Cross-user PII read of Seller's UserProfile must fail with PERMISSION_DENIED
        val crossProfileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            consumerRepo.getUserProfile(sellerUid)
        }
        assertTrue(crossProfileResult.isFailure)
        val crossEx = crossProfileResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(crossEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, crossEx?.code)

        // Consumer submits proximity-verified crowd report
        val reportResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            consumerRepo.submitCrowdQueueReport(
                station = station,
                reporterAlias = "Ruwan",
                reportedStatus = "AVAILABLE",
                queueLengthEstimate = 18,
                comment = "Queue moving steadily",
                userLatitude = 6.8515,
                userLongitude = 79.8658
            )
        }
        assertTrue("Expected crowd report success: ${reportResult.exceptionOrNull()}", reportResult.isSuccess)

        // Consumer subscribes to Watchlist
        val watchResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            consumerRepo.toggleWatchlist(station, CylinderSizeFilter.SIZE_12_5KG, currentlyWatched = false)
        }
        assertTrue("Expected watchlist toggle success: ${watchResult.exceptionOrNull()}", watchResult.isSuccess)

        val watchedItems = withTimeout(FLOW_TIMEOUT_MS) {
            consumerRepo.observeWatchlist(consumerUid).first { it.isNotEmpty() }
        }
        assertEquals(stationId, watchedItems.first().stationId)
    }

    @Test
    fun observePublicDistributors_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = GasRepository(firestore, auth)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observePublicDistributors().first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        assertNotNull("Expected unauthenticated Flow to throw exception", caught)
        val firestoreEx = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
