package com.roadsensevision.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.roadsensevision.data.database.dao.MonitoringSessionDao
import com.roadsensevision.data.database.dao.PotholeObservationDao
import com.roadsensevision.data.database.dao.RoadDefectDao

@Database(
    entities = [
        MonitoringSessionEntity::class,
        RoadDefectEntity::class,
        PotholeObservationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun monitoringSessionDao(): MonitoringSessionDao
    abstract fun roadDefectDao(): RoadDefectDao
    abstract fun potholeObservationDao(): PotholeObservationDao

    companion object {
        const val DATABASE_NAME = "roadsense_vision.db"
    }
}
