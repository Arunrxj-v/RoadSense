package com.roadsensevision.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.roadsensevision.domain.models.Severity
import com.roadsensevision.monitoring.ModelStatus
import com.roadsensevision.monitoring.MonitoringViewModel
import com.roadsensevision.ui.AppViewModel
import com.roadsensevision.ui.components.EvidenceImage
import com.roadsensevision.ui.components.HomeTopBar
import com.roadsensevision.ui.components.MetricTile
import com.roadsensevision.ui.components.PrimaryActionButton
import com.roadsensevision.ui.components.RoadSenseLogo
import com.roadsensevision.ui.components.SectionHeader
import com.roadsensevision.ui.components.SeverityChip
import com.roadsensevision.ui.components.StatusChip
import com.roadsensevision.ui.theme.BodyMd
import com.roadsensevision.ui.theme.HeadlineLg
import com.roadsensevision.ui.theme.Ink
import com.roadsensevision.ui.theme.LabelMd
import com.roadsensevision.ui.theme.LabelSm
import com.roadsensevision.ui.theme.Muted
import com.roadsensevision.ui.theme.Primary
import com.roadsensevision.ui.theme.Secondary
import com.roadsensevision.ui.theme.Slate
import com.roadsensevision.ui.theme.Surface
import com.roadsensevision.ui.theme.SurfaceHigh
import com.roadsensevision.ui.theme.SurfaceLow
import com.roadsensevision.utilities.GeoUtils
import com.roadsensevision.utilities.TimeUtils

@Composable
fun DashboardScreen(
    appViewModel: AppViewModel,
    monitoringViewModel: MonitoringViewModel,
    onStartMonitoring: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDefect: (String) -> Unit,
    onOpenSummary: () -> Unit
) {
    val defects by appViewModel.defects.collectAsState()
    val observationCount by appViewModel.observationCount.collectAsState()
    val latestSession by appViewModel.latestSession.collectAsState()
    val distanceMeters by appViewModel.distanceMeters.collectAsState()
    val monitoringState by monitoringViewModel.ui.collectAsState()

    val modelReady = monitoringState.modelStatus == ModelStatus.READY
    val gpsActive = monitoringState.gpsState.name == "ACTIVE"

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFFBFBFA))
    ) {
        HomeTopBar(onOpenSettings = onOpenSettings)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // Editorial intro
            StatusChip(
                label = if (modelReady && gpsActive) "Sensor suite calibrated"
                else if (modelReady) "Model ready" else "Initializing sensors",
                active = modelReady
            )
            Spacer(Modifier.height(8.dp))
            Text("Understand the roads you travel.", style = HeadlineLg, color = Ink)
            Text(
                "Detect potholes automatically using your phone camera and GPS.",
                style = BodyMd,
                color = Slate
            )

            Spacer(Modifier.height(24.dp))

            // Camera feed / evidence preview card
            val latestDefect = defects.maxByOrNull { it.lastObservedAt }
            val latestEvidence = latestDefect?.let { d ->
                appViewModel.observationForDefect(d.defectId)?.imageRelativePath
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceHigh)
            ) {
                if (latestEvidence != null) {
                    EvidenceImage(
                        relativePath = latestEvidence,
                        loader = appViewModel::loadEvidence,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // No detections yet — neutral placeholder, no fake boxes.
                    Box(
                        Modifier.fillMaxSize().background(SurfaceLow),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Videocam, contentDescription = null, tint = Muted, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Camera preview appears once you start monitoring",
                                style = LabelSm,
                                color = Slate,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                }

                // Top-left: live feed status
                Row(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Surface)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(if (modelReady) Secondary else Muted))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (modelReady) "Model ready · On-device" else "Model loading",
                        style = LabelSm,
                        color = Ink
                    )
                }

                // Top-right: GPS badge
                Row(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Surface)
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.GpsFixed, contentDescription = null, tint = Slate, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (gpsActive) "GPS ${GeoUtils.formatAccuracy(monitoringState.gpsAccuracy)}"
                        else "GPS standby",
                        style = LabelSm,
                        color = Ink
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Primary action
            PrimaryActionButton(
                label = "Start monitoring",
                onClick = onStartMonitoring,
                icon = {
                    Icon(Icons.Filled.Videocam, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(2.dp))
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            )

            Spacer(Modifier.height(24.dp))

            // 3 metrics
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface)
                    .padding(16.dp)
            ) {
                Row(Modifier.fillMaxWidth()) {
                    MetricTile(
                        value = defects.size.toString(),
                        label = "Potholes detected",
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceLow)
                    ) {
                        MetricTile(
                            value = appViewModel.formatKm(distanceMeters),
                            label = "Monitored",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    MetricTile(
                        value = latestSession?.let { TimeUtils.formatClock(it.startedAt) } ?: "—",
                        label = "Last session",
                        valueStyle = com.roadsensevision.ui.theme.HeadlineSm,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Recent detection
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader("Recent detection")
                Row(
                    Modifier.clickable { onOpenHistory() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("View history", style = LabelMd, color = Primary)
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(8.dp))

            val recent = defects.maxByOrNull { it.lastObservedAt }
            if (recent != null) {
                val obs = appViewModel.observationForDefect(recent.defectId)
                val severity = obs?.let { Severity.fromConfidence(it.confidence) } ?: Severity.MODERATE
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface)
                        .clickable { onOpenDefect(recent.defectId) }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (obs != null) {
                            EvidenceImage(
                                relativePath = obs.imageRelativePath,
                                loader = appViewModel::loadEvidence,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(recent.defectId, style = LabelMd.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                SeverityChip(severity)
                            }
                            Text(
                                if (obs != null) "${(obs.confidence * 100).toInt()}% confidence · ${TimeUtils.formatTime(obs.observedAt)}"
                                else "Observation recorded",
                                style = LabelSm,
                                color = Slate
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Slate, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    GeoUtils.formatCoordinate(recent.latestLatitude, recent.latestLongitude),
                                    style = LabelSm,
                                    color = Slate,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Slate, modifier = Modifier.size(20.dp))
                    }
                }
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No detections yet. Start monitoring to record potholes.",
                        style = BodyMd,
                        color = Slate,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Calibration notice
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceLow)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(Surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Secondary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            if (modelReady) "Optimal dash angle ready" else "Preparing detection engine",
                            style = LabelMd,
                            color = Ink
                        )
                        Text(
                            if (modelReady) "Camera and GPS are ready for vibration-proof road logging."
                            else "The on-device model is initializing. Start monitoring becomes available when ready.",
                            style = LabelSm,
                            color = Slate
                        )
                    }
                }
            }

            // Session summary entry (when a completed session exists)
            if (latestSession != null && latestSession!!.status == "COMPLETED") {
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Surface)
                        .clickable { onOpenSummary() }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Session summary", style = com.roadsensevision.ui.theme.HeadlineSm, color = Ink)
                        Text(
                            "View the latest completed session report",
                            style = LabelSm,
                            color = Slate
                        )
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Slate, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
