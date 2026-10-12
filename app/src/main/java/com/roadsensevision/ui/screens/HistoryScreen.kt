package com.roadsensevision.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.roadsensevision.data.database.RoadDefectEntity
import com.roadsensevision.domain.models.Severity
import com.roadsensevision.ui.AppViewModel
import com.roadsensevision.ui.components.DetailTopBar
import com.roadsensevision.ui.components.EvidenceImage
import com.roadsensevision.ui.components.EmptyState
import com.roadsensevision.ui.components.SeverityChip
import com.roadsensevision.ui.components.StatusChip
import com.roadsensevision.ui.theme.HeadlineLg
import com.roadsensevision.ui.theme.Ink
import com.roadsensevision.ui.theme.LabelLg
import com.roadsensevision.ui.theme.LabelMd
import com.roadsensevision.ui.theme.LabelSm
import com.roadsensevision.ui.theme.Primary
import com.roadsensevision.ui.theme.Slate
import com.roadsensevision.ui.theme.Surface
import com.roadsensevision.ui.theme.SurfaceHigh
import com.roadsensevision.ui.theme.SurfaceLow
import com.roadsensevision.utilities.GeoUtils
import com.roadsensevision.utilities.TimeUtils

@Composable
fun HistoryScreen(
    appViewModel: AppViewModel,
    onOpenDefect: (String) -> Unit
) {
    val defects by appViewModel.defects.collectAsState()
    val observationCount by appViewModel.observationCount.collectAsState()
    val context = LocalContext.current

    var query by remember { mutableStateOf("") }
    var severityFilter by remember { mutableStateOf<Severity?>(null) }

    // Build display rows: unique defects with their best observation.
    val rows = remember(defects, appViewModel.observations.value) {
        defects.mapNotNull { defect ->
            val obs = appViewModel.observationForDefect(defect.defectId)
            val severity = obs?.let { Severity.fromConfidence(it.confidence) } ?: Severity.MODERATE
            Triple(defect, obs, severity)
        }
    }

    val filtered = rows.filter { (defect, obs, severity) ->
        val q = query.trim().lowercase()
        val matchesQuery = q.isEmpty() ||
            defect.defectId.lowercase().contains(q) ||
            GeoUtils.formatCoordinate(defect.latestLatitude, defect.latestLongitude).lowercase().contains(q)
        val matchesSeverity = severityFilter == null || severity == severityFilter
        matchesQuery && matchesSeverity
    }.sortedByDescending { it.first.lastObservedAt }

    Column(Modifier.fillMaxSize().background(Color(0xFFFBFBFA))) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            // Header
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Detection history", style = HeadlineLg, color = Ink)
                StatusChip(label = "Saved locally", active = true)
            }
            Text(
                "$observationCount potholes automatically recorded",
                style = com.roadsensevision.ui.theme.BodyMd,
                color = Slate
            )

            Spacer(Modifier.height(16.dp))

            // Search
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by ID or coordinate...", style = com.roadsensevision.ui.theme.BodyMd, color = com.roadsensevision.ui.theme.Muted) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = com.roadsensevision.ui.theme.Muted) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface,
                    unfocusedBorderColor = SurfaceHigh,
                    focusedBorderColor = Ink
                )
            )

            Spacer(Modifier.height(12.dp))

            // Severity filter chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill("All", severityFilter == null) { severityFilter = null }
                FilterPill("Low", severityFilter == Severity.LOW) { severityFilter = Severity.LOW }
                FilterPill("Moderate", severityFilter == Severity.MODERATE) { severityFilter = Severity.MODERATE }
                FilterPill("Severe", severityFilter == Severity.SEVERE) { severityFilter = Severity.SEVERE }
            }

            Spacer(Modifier.height(12.dp))

            // Summary bar
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceLow).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${filtered.size} unique defects", style = LabelMd, color = Slate)
                Text("Offline · no sync", style = LabelSm, color = Primary)
            }

            Spacer(Modifier.height(8.dp))
        }

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = { Icon(Icons.Filled.History, contentDescription = null, tint = com.roadsensevision.ui.theme.Muted, modifier = Modifier.size(28.dp)) },
                    title = "No detections yet",
                    subtitle = if (defects.isEmpty())
                        "Recorded potholes will appear here after you start monitoring."
                    else "No defects match your search or filter."
                )
            }
        } else {
            LazyColumn(
                Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.first.defectId }) { (defect, obs, severity) ->
                    DefectRow(defect = defect, confidence = obs?.confidence, severity = severity,
                        evidencePath = obs?.imageRelativePath, loader = appViewModel::loadEvidence,
                        onClick = { onOpenDefect(defect.defectId) })
                }

                // Export
                item {
                    Spacer(Modifier.height(4.dp))
                    val defectsToExport = filtered.map { it.first }
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Surface)
                                .clickable { exportCsv(context, defectsToExport) },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Export CSV / GeoJSON", style = LabelLg, color = Ink)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Local export · compatible with QGIS, ArcGIS, OpenStreetMap",
                            style = LabelSm,
                            color = com.roadsensevision.ui.theme.Muted
                        )
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) com.roadsensevision.ui.theme.FilterSelected else Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(label, style = LabelMd, color = if (selected) Color.White else Slate)
    }
}

@Composable
private fun DefectRow(
    defect: RoadDefectEntity,
    confidence: Float?,
    severity: Severity,
    evidencePath: String?,
    loader: (String) -> android.graphics.Bitmap?,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (evidencePath != null) {
            EvidenceImage(relativePath = evidencePath, loader = loader, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)))
        } else {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceHigh))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(defect.defectId, style = LabelLg.copy(fontWeight = FontWeight.SemiBold), color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (confidence != null) {
                    Text("${(confidence * 100).toInt()}% confidence", style = LabelSm, color = Primary,
                        modifier = Modifier.background(Color(0xFFFFEDD5), RoundedCornerShape(50)).padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Spacer(Modifier.weight(1f))
                SeverityChip(severity)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                "${TimeUtils.formatTime(defect.lastObservedAt)} · ${defect.observationCount} observation(s)",
                style = com.roadsensevision.ui.theme.BodySm,
                color = Slate
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = com.roadsensevision.ui.theme.Muted, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(2.dp))
                Text(
                    GeoUtils.formatCoordinate(defect.latestLatitude, defect.latestLongitude),
                    style = LabelSm,
                    color = Slate,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = com.roadsensevision.ui.theme.Muted, modifier = Modifier.size(20.dp))
    }
}

private fun exportCsv(context: android.content.Context, defects: List<RoadDefectEntity>) {
    val sb = StringBuilder("defectId,latitude,longitude,gpsAccuracy,observations,status,firstDetected,lastObserved\n")
    for (d in defects) {
        sb.append("${d.defectId},${d.latestLatitude ?: ""},${d.latestLongitude ?: ""},${d.latestGpsAccuracy ?: ""},")
        sb.append("${d.observationCount},${d.status},${d.firstDetectedAt},${d.lastObservedAt}\n")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_TEXT, sb.toString())
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Export detection data"))
    } catch (_: Exception) {}
}
