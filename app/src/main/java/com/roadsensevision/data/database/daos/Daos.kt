package com.roadsensevision.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.roadsensevision.data.database.MonitoringSessionEntity
import com.roadsensevision.data.database.PotholeObservationEntity
import com.roadsensevision.data.database.RoadDefectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitoringSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: MonitoringSessionEntity)

    @Update
    suspend fun update(session: MonitoringSessionEntity)

    @Query("SELECT * FROM monitoring_sessions ORDER BY startedAt DESC")
    fun observeSessions(): Flow<List<MonitoringSessionEntity>>

    @Query("SELECT * FROM monitoring_sessions WHERE sessionId = :sessionId")
    suspend fun getSession(sessionId: String): MonitoringSessionEntity?

    @Query("SELECT * FROM monitoring_sessions ORDER BY startedAt DESC LIMIT 1")
    fun observeLatestSession(): Flow<MonitoringSessionEntity?>
}

@Dao
interface RoadDefectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(defect: RoadDefectEntity)

    @Query("SELECT * FROM road_defects WHERE defectId = :defectId")
    suspend fun getDefect(defectId: String): RoadDefectEntity?

    @Query("SELECT * FROM road_defects ORDER BY lastObservedAt DESC")
    fun observeDefects(): Flow<List<RoadDefectEntity>>

    @Query("SELECT COUNT(*) FROM road_defects")
    suspend fun getDefectCount(): Int

    @Query("SELECT * FROM road_defects ORDER BY lastObservedAt DESC LIMIT :limit")
    suspend fun getRecentDefects(limit: Int): List<RoadDefectEntity>

    @Query("SELECT COUNT(*) FROM road_defects")
    fun observeDefectCount(): Flow<Int>
}

@Dao
interface PotholeObservationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(observation: PotholeObservationEntity)

    @Query("SELECT * FROM pothole_observations ORDER BY observedAt DESC")
    fun observeObservations(): Flow<List<PotholeObservationEntity>>

    @Query("SELECT * FROM pothole_observations WHERE observationId = :observationId")
    suspend fun getObservation(observationId: String): PotholeObservationEntity?

    @Query("SELECT * FROM pothole_observations WHERE defectId = :defectId ORDER BY observedAt ASC")
    fun observeObservationsForDefect(defectId: String): Flow<List<PotholeObservationEntity>>

    @Query("SELECT COUNT(*) FROM pothole_observations")
    fun observeObservationCount(): Flow<Int>

    @Query("SELECT * FROM pothole_observations WHERE defectId = :defectId ORDER BY confidence DESC LIMIT 1")
    suspend fun getBestObservationForDefect(defectId: String): PotholeObservationEntity?

    @Query("""
        SELECT o.observationId AS observationId, o.defectId AS defectId, o.sessionId AS sessionId,
               o.observedAt AS observedAt, o.latitude AS latitude, o.longitude AS longitude,
               o.gpsAccuracy AS gpsAccuracy, o.confidence AS confidence,
               o.imageRelativePath AS imageRelativePath, o.locationQuality AS locationQuality,
               o.synchronizationStatus AS synchronizationStatus
        FROM pothole_observations o WHERE o.sessionId = :sessionId ORDER BY o.observedAt DESC
    """)
    suspend fun getObservationDetailsForSession(sessionId: String): List<ObservationDetail>
}

/** Observation joined with its defect, for display. */
data class ObservationDetail(
    val observationId: String,
    val defectId: String,
    val sessionId: String,
    val observedAt: Long,
    val latitude: Double?,
    val longitude: Double?,
    val gpsAccuracy: Float?,
    val confidence: Float,
    val imageRelativePath: String,
    val locationQuality: String,
    val synchronizationStatus: String
)
