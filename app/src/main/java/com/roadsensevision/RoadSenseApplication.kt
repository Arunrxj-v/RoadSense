package com.roadsensevision

import android.app.Application
import com.roadsensevision.data.sync.SyncScheduler

class RoadSenseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SyncScheduler.ensureScheduled(this)
        SyncScheduler.requestImmediateSync(this)
    }
}
