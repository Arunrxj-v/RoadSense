package com.roadsensevision.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roadsensevision.ui.AppViewModel
import com.roadsensevision.ui.components.DetailTopBar
import com.roadsensevision.ui.components.EvidenceImage
import com.roadsensevision.ui.components.MarkerData
import com.roadsensevision.ui.components.OsmdroidMap
import com.roadsensevision.ui.components.StatusChip
import com.roadsensevision.ui.theme.HeadlineLg
import com.roadsensevision.ui.theme.HeadlineSm
import com.roadsensevision.ui.theme.Ink
import com.roadsensevision.ui.theme.LabelLg
import com.roadsensevision.ui.theme.LabelMd
import com.roadsensevision.ui.theme.LabelSm
import com.roadsensevision.ui.theme.Primary
import com.roadsensevision.ui.theme.Secondary
import com.roadsensevision.ui.theme.Slate
import com.roadsensevision.ui.theme.Surface
import com.roadsensevision.ui.theme.SurfaceHigh
import com.roadsensevision.ui.theme.SurfaceLow
import com.roadsensevision.utilities.GeoUtils
import com.roadsensevision.utilities.TimeUtils
import java.util.Locale

@Composable
fun SessionSummaryScreen(
    appViewModel: AppViewModel,
    onOpenMap: () -> Unit,
    onBack: () -> Unit
) {
    val latestSession by appViewModel.latestSession.collectAsState()
    val observations by appViewModel.observations.collectAsState()
    val defects by appViewModel.defects.collectAsState()
    val syncSummary by appViewModel.syncSummary.collectAsState()

    val session = latestSession
    val sessionObs = session?.let { s ->
        observations.filter { it.sessionId == s.sessionId }.sortedBy { it.observedAt }
    } ?: emptyList()

    Column(Modifier.fillMaxSize().background(Color(0xFFFBFBFA))) {
        DetailTopBar(title = "Session summary", onBack = onBack)

        if (session == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Filled.Route, contentDescription = null, tint = com.roadsensevision.ui.theme.Muted, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No sessions yet", style = HeadlineSm, color = Ink)
                    Spacer(Modifier.height(4.dp))
                    Text("Complete a monitoring session to see its report here.", style = LabelSm, color = Slate)
                }
            }
            return@Column
        }

        val durationMs = (session.endedAt ?: System.currentTimeMillis()) - session.startedAt
        val uniqueDefects = sessionObs.map { it.defectId }.distinct()
        val sessionKm = if (sessionObs.size >= 2) {
            sessionObs.zipWithNext { a, b ->
                if (a.latitude != null && a.longitude != null && b.latitude != null && b.longitude != null)
                    GeoUtils.distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)
                else 0.0
            }.sum() / 1000.0
        } else 0.0
        val gpsAccuracies = sessionObs.mapNotNull { it.gpsAccuracy }
        val avgGps = gpsAccuracies.takeIf { it.isNotEmpty() }?.average()
        val stats = parseStats(session.processingStatistics)

        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            item {
                Spacer(Modifier.height(16.dp))
                // Header card
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface).padding(16.dp)) {
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            StatusChip(label = "Inspection verified", active = true)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Save, contentDescription = null, tint = Slate, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    when {
                                        !syncSummary.configured -> "Saved locally"
                                        syncSummary.uploading -> "Saved locally · uploading queued"
                                        syncSummary.pending + syncSummary.failed > 0 ->
                                            "Saved locally · ${syncSummary.pending + syncSummary.failed} queued"
                                        syncSummary.noLocation > 0 ->
                                            "Saved locally · ${syncSummary.noLocation} without GPS"
                                        else -> "Synced to server"
                                    },
                                    style = LabelMd,
                                    color = Slate
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Session complete", style = HeadlineLg, color = Ink)
                        Text(
                            "Session ${session.sessionId} · ${TimeUtils.formatDateTime(session.startedAt)}",
                            style = com.roadsensevision.ui.theme.BodyMd, color = Slate
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                // Key metrics strip
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface).padding(16.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        MetricColumn(sessionObs.map { it.defectId }.distinct().size.toString(), "Detections", Modifier.weight(1f), true)
                        MetricColumn(String.format(Locale.US, "%.1f", sessionKm), "km monitored", Modifier.weight(1f))
                        MetricColumn(TimeUtils.formatMinutes(durationMs).toString(), "min duration", Modifier.weight(1f))
                    }
                }

                Spacer(Modifier.height(12.dp))
                // GPS + performance
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface).padding(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Field telemetry", style = LabelLg, color = Ink)
                        TelemetryLine("GPS quality", avgGps?.let { "Avg accuracy ${GeoUtils.formatAccuracy(it.toFloat())}" } ?: "Unavailable")
                        TelemetryLine("Inference", if (stats.fps > 0f) String.format(Locale.US, "%.1f FPS · %.0f ms avg", stats.fps, stats.latency) else "Not recorded")
                        TelemetryLine("Frames processed", stats.frames.toString())
                        TelemetryLine("Observations recorded", sessionObs.size.toString())
                        TelemetryLine("Unique defects", uniqueDefects.size.toString())
                    }
                }

                Spacer(Modifier.height(12.dp))
                // Tracked route map
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface)) {
                    Column {
                        Row(Modifier.fillMaxWidth().padding(16.dp, 16.dp, 16.dp, 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Route, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Tracked route", style = HeadlineSm, color = Ink)
                            }
                            Text(String.format(Locale.US, "%.1f km traverse", sessionKm), style = LabelSm, color = Slate)
                        }
                        val routeMarkers = sessionObs.filter { it.latitude != null && it.longitude != null }
                            .map { MarkerData(it.defectId, it.latitude!!, it.longitude!!, it.defectId, "${(it.confidence * 100).toInt()}%") }
                        if (routeMarkers.isNotEmpty()) {
                            OsmdroidMap(
                                markers = routeMarkers,
                                initialZoom = 15.0,
                                onMarkerClick = {},
                                modifier = Modifier.fillMaxWidth().height(180.dp)
                            )
                        } else {
                            Box(Modifier.fillMaxWidth().height(180.dp).background(SurfaceLow), contentAlignment = Alignment.Center) {
                                Text("No GPS coordinates in this session", style = LabelSm, color = Slate)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Latest detections", style = HeadlineLg, color = Ink)
                Spacer(Modifier.height(4.dp))
                Text("${sessionObs.size} logged", style = LabelSm, color = Slate)
                Spacer(Modifier.height(8.dp))

                // Integrity note
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SurfaceHigh).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.Verified, contentDescription = null, tint = Ink, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Integrity & telemetry", style = LabelLg, color = Ink)
                            Text(
                                "100% on-device inference · ${stats.inferences} model runs · metadata sync only",
                                style = com.roadsensevision.ui.theme.BodySm, color = Slate
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            items(sessionObs.size) { index ->
                val obs = sessionObs[sessionObs.size - 1 - index]
                val defect = defects.find { it.defectId == obs.defectId }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp))
                        .background(SurfaceLow).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EvidenceImage(
                        relativePath = obs.imageRelativePath,
                        loader = appViewModel::loadEvidence,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(obs.defectId, style = LabelLg.copy(fontWeight = FontWeight.SemiBold), color = Ink)
                        Text(
                            "${TimeUtils.formatTime(obs.observedAt)} · ${obs.locationQuality} GPS",
                            style = com.roadsensevision.ui.theme.BodySm, color = Slate
                        )
                        if (defect != null && defect.latestLatitude != null) {
                            Text(GeoUtils.formatCoordinate(defect.latestLatitude, defect.latestLongitude), style = LabelSm, color = Slate)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${(obs.confidence * 100).toInt()}%", style = LabelMd.copy(fontWeight = FontWeight.SemiBold), color = Primary)
                        Text("Confidence", style = LabelSm, color = Slate)
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(8.dp)).background(Primary).clickable { onOpenMap() },
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Map, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("View on map", style = LabelLg, color = Color.White)
                    }
                    Row(
                        Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceHigh).clickable { onBack() },
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Home, contentDescription = null, tint = Ink, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Back to home", style = LabelLg, color = Ink)
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun MetricColumn(value: String, label: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = HeadlineLg.copy(fontWeight = FontWeight.Bold, color = if (highlight) Primary else Ink)
        )
        Text(label, style = LabelMd, color = Slate)
    }
}

@Composable
private fun TelemetryLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = com.roadsensevision.ui.theme.BodySm, color = Slate)
        Text(value, style = LabelMd.copy(fontWeight = FontWeight.Medium), color = Ink)
    }
}

/** Minimal parser for the stored processing-statistics JSON. */
private fun parseStats(json: String): SessionStatsData {
    return SessionStatsData(
        fps = extract(json, "fps"),
        latency = extract(json, "avgLatencyMs"),
        frames = extract(json, "framesProcessed").toLong(),
        inferences = extract(json, "inferenceCount").toLong()
    )
}

private fun extract(json: String, key: String): Float {
    val match = Regex("\"$key\":\\s*([-0-9.]+)").find(json)
    return match?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
}

private data class SessionStatsData(
    val fps: Float,
    val latency: Float,
    val frames: Long,
    val inferences: Long
)
