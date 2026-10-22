package com.roadsensevision.data.sync

data class SyncSummary(
    val pending: Int = 0,
    val synced: Int = 0,
    val failed: Int = 0,
    val noLocation: Int = 0,
    val configured: Boolean = false,
    val uploading: Boolean = false,
) {
    val total: Int get() = pending + synced + failed + noLocation
}
