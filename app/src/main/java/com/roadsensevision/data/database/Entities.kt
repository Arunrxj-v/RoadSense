package com.roadsensevision.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.roadsensevision.domain.models.SessionStatus
import com.roadsensevision.domain.models.SyncStatus

@Entity(
    tableName = "monitoring_sessions",
    indices = [Index(value = ["startedAt"])],
)
data class MonitoringSessionEntity(
    @PrimaryKey val sessionId: String,
    val startedAt: Long,
    val endedAt: Long?,
    val status: String,
    val observationCount: Int,
    val uniqueDefectCount: Int,
    val processingStatistics: String,
)

@Entity(
    tableName = "road_defects",
    indices = [
        Index(value = ["latestLatitude", "latestLongitude"]),
        Index(value = ["lastObservedAt"]),
    ],
)
data class RoadDefectEntity(
    @PrimaryKey val defectId: String,
    val firstDetectedAt: Long,
    val lastObservedAt: Long,
    val latestLatitude: Double?,
    val latestLongitude: Double?,
    val latestGpsAccuracy: Float?,
    val observationCount: Int,
    val status: String,
    val lastVerifiedAt: Long?,
)

@Entity(
    tableName = "pothole_observations",
    indices = [
        Index(value = ["defectId"]),
        Index(value = ["sessionId"]),
        Index(value = ["observedAt"]),
        Index(value = ["latitude", "longitude"]),
    ],
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = RoadDefectEntity::class,
            parentColumns = ["defectId"],
            childColumns = ["defectId"],
            onDelete = androidx.room.ForeignKey.CASCADE,
        ),
        androidx.room.ForeignKey(
            entity = MonitoringSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = androidx.room.ForeignKey.CASCADE,
        ),
    ],
)
data class PotholeObservationEntity(
    @PrimaryKey val observationId: String,
    val defectId: String,
    val sessionId: String,
    val observedAt: Long,
    val latitude: Double?,
    val longitude: Double?,
    val gpsAccuracy: Float?,
    val confidence: Float,
    val imageRelativePath: String?,
    val locationQuality: String,
    val synchronizationStatus: String,
    val serverRecordId: String?,
)
