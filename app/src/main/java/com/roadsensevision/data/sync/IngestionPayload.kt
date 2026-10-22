package com.roadsensevision.data.sync

data class IngestionPayload(
    val reportId: String,
    val latitude: Double,
    val longitude: Double,
    val observedAt: String,
    val confidence: Float?,
    val gpsAccuracyM: Float?,
) {
    fun toJson(): String {
        val fields = mutableListOf(
            "\"report_id\":${quote(reportId)}",
            "\"latitude\":$latitude",
            "\"longitude\":$longitude",
            "\"observed_at\":${quote(observedAt)}"
        )
        if (confidence != null) fields += "\"confidence\":$confidence"
        if (gpsAccuracyM != null) fields += "\"gps_accuracy_m\":$gpsAccuracyM"
        return fields.joinToString(separator = ",", prefix = "{", postfix = "}")
    }

    private fun quote(value: String): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }
}
