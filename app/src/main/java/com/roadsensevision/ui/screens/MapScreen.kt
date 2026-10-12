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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddRoad
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.roadsensevision.domain.models.Severity
import com.roadsensevision.ui.AppViewModel
import com.roadsensevision.ui.components.EvidenceImage
import com.roadsensevision.ui.components.MarkerData
import com.roadsensevision.ui.components.OsmdroidMap
import com.roadsensevision.ui.components.SeverityChip
import com.roadsensevision.ui.theme.HeadlineLg
import com.roadsensevision.ui.theme.HeadlineSm
import com.roadsensevision.ui.theme.Ink
import com.roadsensevision.ui.theme.LabelLg
import com.roadsensevision.ui.theme.LabelMd
import com.roadsensevision.ui.theme.LabelSm
import com.roadsensevision.ui.theme.Muted
import com.roadsensevision.ui.theme.Primary
import com.roadsensevision.ui.theme.Secondary
import com.roadsensevision.ui.theme.Slate
import com.roadsensevision.ui.theme.Surface
import com.roadsensevision.ui.theme.SurfaceHigh
import com.roadsensevision.utilities.GeoUtils
import com.roadsensevision.utilities.TimeUtils

@Composable
fun MapScreen(
    appViewModel: AppViewModel,
    onOpenDefect: (String) -> Unit
) {
    val defects by appViewModel.defects.collectAsState()
    val observations by appViewModel.observations.collectAsState()

    var severityFilter by remember { mutableStateOf<Severity?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var recenter by remember { mutableIntStateOf(0) }

    // Build markers with severity (from best observation per defect).
    val entries = defects.mapNotNull { defect ->
        val obs = appViewModel.observationForDefect(defect.defectId)
        val severity = obs?.let { Severity.fromConfidence(it.confidence) } ?: Severity.MODERATE
        Triple(defect, obs, severity)
    }
    val visible = entries.filter { (_, _, severity) -> severityFilter == null || severity == severityFilter }

    val markers = visible.mapNotNull { (defect, obs, _) ->
        val lat = defect.latestLatitude
        val lon = defect.latestLongitude
        if (lat == null || lon == null) null
        else MarkerData(
            id = defect.defectId,
            latitude = lat,
            longitude = lon,
            title = defect.defectId,
            snippet = "${obs?.confidence?.let { (it * 100).toInt() } ?: 0}% · ${TimeUtils.formatClock(defect.lastObservedAt)}"
        )
    }

    val selectedEntry = entries.find { it.first.defectId == selectedId }
    val selectedDefect = selectedEntry?.first
    val selectedObs = selectedEntry?.second
    val selectedSeverity = selectedEntry?.third ?: Severity.MODERATE

    Box(Modifier.fillMaxSize()) {
        // Map fills the viewport
        OsmdroidMap(
            markers = markers,
            selectedId = selectedId,
            onMarkerClick = { marker -> selectedId = marker.id },
            recenterToken = recenter,
            modifier = Modifier.fillMaxSize()
        )

        // Top summary pill
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(50))
                .background(Surface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFFFEDD5)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.AddRoad, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Road observations", style = HeadlineSm, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Secondary))
                }
                Text(
                    "${visible.size} potholes · ${entries.size} total",
                    style = LabelSm, color = Slate, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(SurfaceHigh).clickable { recenter++ },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Ink, modifier = Modifier.size(16.dp)) }
        }

        // Legend note
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 76.dp)
                .clip(RoundedCornerShape(50))
                .background(Surface)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.width(16.dp).height(3.dp).background(Primary))
            Spacer(Modifier.width(8.dp))
            Text("Pothole markers", style = LabelSm, color = Slate)
            Text(" · Tap for details", style = LabelSm, color = Muted)
        }

        // Severity filter chips
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 118.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MapFilter("All", severityFilter == null) { severityFilter = null }
            MapFilter("Low", severityFilter == Severity.LOW) { severityFilter = Severity.LOW }
            MapFilter("Moderate", severityFilter == Severity.MODERATE) { severityFilter = Severity.MODERATE }
            MapFilter("Severe", severityFilter == Severity.SEVERE) { severityFilter = Severity.SEVERE }
        }

        // Recenter FAB
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = if (selectedDefect != null) 240.dp else 24.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Surface)
                .clickable { recenter++ },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.MyLocation, contentDescription = "Recenter", tint = Primary, modifier = Modifier.size(22.dp)) }

        // Empty state
        if (defects.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xCCFFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Surface).padding(24.dp)
                ) {
                    Icon(Icons.Filled.Map, contentDescription = null, tint = Muted, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No observations yet", style = HeadlineSm, color = Ink)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Start monitoring to record potholes and place them on this map.",
                        style = LabelSm, color = Slate, modifier = Modifier.width(220.dp)
                    )
                }
            }
        }

        // Bottom sheet for selected defect
        selectedDefect?.let { defect ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface)
                    .padding(16.dp)
            ) {
                Box(Modifier.width(36.dp).height(4.dp).align(Alignment.CenterHorizontally).background(SurfaceHigh))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Primary))
                    Spacer(Modifier.width(8.dp))
                    Text("Detection ${defect.defectId}", style = HeadlineSm, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(8.dp))
                    SeverityChip(selectedSeverity)
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).background(SurfaceHigh).clickable { selectedId = null },
                        contentAlignment = Alignment.Center
                    ) { Text("×", style = LabelLg, color = Slate) }
                }

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedObs != null) {
                        EvidenceImage(
                            relativePath = selectedObs.imageRelativePath,
                            loader = appViewModel::loadEvidence,
                            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp))
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Time", style = LabelSm, color = Muted, modifier = Modifier.width(64.dp))
                            Text(TimeUtils.formatDateTime(defect.lastObservedAt), style = LabelMd, color = Ink)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Coordinates", style = LabelSm, color = Muted, modifier = Modifier.width(64.dp))
                            Text(
                                GeoUtils.formatCoordinate(defect.latestLatitude, defect.latestLongitude),
                                style = LabelMd, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Confidence", style = LabelSm, color = Muted, modifier = Modifier.width(64.dp))
                            Text(
                                selectedObs?.let { "${(it.confidence * 100).toInt()}% · on-device" } ?: "—",
                                style = LabelMd.copy(fontWeight = FontWeight.SemiBold), color = Primary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(8.dp)).background(Primary)
                        .clickable { onOpenDefect(defect.defectId) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("View detection details", style = LabelLg, color = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun MapFilter(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = LabelMd,
        color = if (selected) Color.White else Ink,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) com.roadsensevision.ui.theme.FilterSelected else Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
