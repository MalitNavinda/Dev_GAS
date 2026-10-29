package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AppLanguage
import com.example.data.model.CylinderSizeFilter
import com.example.data.model.Distributor
import com.example.data.model.QueueLevel
import com.example.ui.i18n.LankaGasTranslations
import com.example.util.GeoUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app_name and firestore_database_id resources are configured`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("LankaGas Live", context.getString(R.string.app_name))
        val dbId = context.getString(R.string.firestore_database_id)
        assertTrue("Expected provisioned Firestore database ID", dbId.isNotBlank())
    }

    @Test
    fun `geoutils computes Sri Lanka haversine distance and geohash accurately`() {
        // Colombo (6.9271, 79.8612) to Kandy (7.2906, 80.6337) is ~94 km straight-line
        val distColomboToKandy = GeoUtils.haversineDistanceKm(6.9271, 79.8612, 7.2906, 80.6337)
        assertTrue(distColomboToKandy in 90.0..100.0)

        assertTrue(GeoUtils.isWithinProximity(6.9271, 79.8612, 6.9300, 79.8650, thresholdKm = 1.5))
        assertFalse(GeoUtils.isWithinProximity(6.9271, 79.8612, 7.2906, 80.6337, thresholdKm = 1.5))

        val geohash = GeoUtils.encodeGeohash(6.9271, 79.8612, precision = 6)
        assertEquals(6, geohash.length)
    }

    @Test
    fun `distributor cylinder filter and crowd discrepancy rules evaluate deterministically`() {
        val station = Distributor(
            id = "colombo-litro-1",
            name = "Union Place Litro Point",
            brand = "LITRO",
            status = "AVAILABLE",
            isVerified = true,
            stock12_5kg = 40L,
            stock5kg = 0L,
            stock2_3kg = 12L,
            confirmationsCount = 1L,
            discrepancyCount = 4L
        )

        assertEquals(52L, station.totalCylinders)
        assertTrue(station.matchesCylinderFilter(CylinderSizeFilter.SIZE_12_5KG))
        assertFalse(station.matchesCylinderFilter(CylinderSizeFilter.SIZE_5KG))
        assertTrue(station.matchesCylinderFilter(CylinderSizeFilter.SIZE_2_3KG))
        assertTrue(station.hasCrowdDiscrepancy)
        assertEquals(QueueLevel.MODERATE, QueueLevel.fromCount(28))
    }

    @Test
    fun `trilingual strings provide English Sinhala and Tamil copy`() {
        val en = LankaGasTranslations.forLanguage(AppLanguage.EN)
        val si = LankaGasTranslations.forLanguage(AppLanguage.SI)
        val ta = LankaGasTranslations.forLanguage(AppLanguage.TA)

        assertTrue(en.signInButton.contains("Google"))
        assertTrue(si.tabExplore.isNotBlank() && si.tabExplore != en.tabExplore)
        assertTrue(ta.tabExplore.isNotBlank() && ta.tabExplore != en.tabExplore)
    }
}
