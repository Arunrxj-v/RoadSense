package com.roadsensevision.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ObservationSyncStateTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun updatingDefectDoesNotCascadeDeleteSyncedObservation() = runBlocking {
        val now = 1_792_617_250_555L
        db.monitoringSessionDao().insert(
            MonitoringSessionEntity("RS-TEST", now, null, "ACTIVE", 0, 0, "{}")
        )
        db.roadDefectDao().insert(
            RoadDefectEntity("PH-00001", now, now, 9.0131503, 76.541354, 34.5f, 1, "ACTIVE", null)
        )
        db.potholeObservationDao().insert(
            PotholeObservationEntity(
                observationId = "obs-synced",
                defectId = "PH-00001",
                sessionId = "RS-TEST",
                observedAt = now,
                latitude = 9.0131503,
                longitude = 76.541354,
                gpsAccuracy = 34.5f,
                confidence = 0.9f,
                imageRelativePath = null,
                locationQuality = "POOR",
                synchronizationStatus = "SYNCED",
                serverRecordId = "PH-000002",
            )
        )

        val existing = db.roadDefectDao().getDefect("PH-00001")
        assertNotNull(existing)
        db.roadDefectDao().update(
            existing!!.copy(lastObservedAt = now + 1_000, observationCount = 2)
        )

        val stored = db.potholeObservationDao().getObservation("obs-synced")
        assertNotNull(stored)
        assertEquals("SYNCED", stored!!.synchronizationStatus)
        assertEquals("PH-000002", stored.serverRecordId)
        assertEquals(1, db.potholeObservationDao().observeObservationsForDefect("PH-00001").first().size)
    }

    @Test
    fun acknowledgementTransitionIsPersisted() = runBlocking {
        val now = 1_792_617_250_555L
        db.monitoringSessionDao().insert(
            MonitoringSessionEntity("RS-TEST", now, null, "ACTIVE", 0, 0, "{}")
        )
        db.roadDefectDao().insert(
            RoadDefectEntity("PH-00001", now, now, 9.0131503, 76.541354, 5f, 1, "ACTIVE", null)
        )
        db.potholeObservationDao().insert(
            PotholeObservationEntity(
                observationId = "obs-pending",
                defectId = "PH-00001",
                sessionId = "RS-TEST",
                observedAt = now,
                latitude = 9.0131503,
                longitude = 76.541354,
                gpsAccuracy = 5f,
                confidence = 0.9f,
                imageRelativePath = null,
                locationQuality = "GOOD",
                synchronizationStatus = "PENDING",
                serverRecordId = null,
            )
        )

        db.potholeObservationDao().updateSyncState("obs-pending", "SYNCED", "PH-000002")

        val stored = db.potholeObservationDao().getObservation("obs-pending")
        assertEquals("SYNCED", stored!!.synchronizationStatus)
        assertEquals("PH-000002", stored.serverRecordId)
    }

    @Test
    fun observationsWithoutCoordinatesAreNotCountedAsUploadFailures() = runBlocking {
        val now = 1_792_617_250_555L
        db.monitoringSessionDao().insert(
            MonitoringSessionEntity("RS-TEST", now, null, "ACTIVE", 0, 0, "{}")
        )
        db.roadDefectDao().insert(
            RoadDefectEntity("PH-00001", now, now, null, null, null, 1, "ACTIVE", null)
        )
        db.potholeObservationDao().insert(
            PotholeObservationEntity(
                observationId = "obs-no-gps",
                defectId = "PH-00001",
                sessionId = "RS-TEST",
                observedAt = now,
                latitude = null,
                longitude = null,
                gpsAccuracy = null,
                confidence = 0.9f,
                imageRelativePath = null,
                locationQuality = "UNAVAILABLE",
                synchronizationStatus = "FAILED",
                serverRecordId = null,
            )
        )

        assertEquals(0, db.potholeObservationDao().observeFailedUploadCount().first())
        assertEquals(1, db.potholeObservationDao().observeNoLocationCount().first())
    }
}
