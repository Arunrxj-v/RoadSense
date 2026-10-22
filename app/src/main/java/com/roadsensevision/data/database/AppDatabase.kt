package com.roadsensevision.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
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

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME,
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
