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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
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
import com.roadsensevision.domain.models.Severity
import com.roadsensevision.ui.AppViewModel
import com.roadsensevision.ui.components.DetailTopBar
import com.roadsensevision.ui.components.EvidenceImage
import com.roadsensevision.ui.components.SeverityChip
import com.roadsensevision.ui.theme.BodyMd
import com.roadsensevision.ui.theme.BodySm
import com.roadsensevision.ui.theme.HeadlineSm
import com.roadsensevision.ui.theme.Ink
import com.roadsensevision.ui.theme.LabelLg
import com.roadsensevision.ui.theme.LabelMd
import com.roadsensevision.ui.theme.LabelSm
import com.roadsensevision.ui.theme.Muted
import com.roadsensevision.ui.theme.Primary
import com.roadsensevision.ui.theme.Slate
import com.roadsensevision.ui.theme.Surface
import com.roadsensevision.ui.theme.SurfaceHigh
import com.roadsensevision.ui.theme.SurfaceLow
import com.roadsensevision.utilities.GeoUtils
import com.roadsensevision.utilities.TimeUtils

@Composable
fun DefectDetailScreen(
    appViewModel: AppViewModel,
    defectId: String,
    onBack: () -> Unit,
    onOpenMap: () -> Unit
) {
    val defects by appViewModel.defects.collectAsState()
    val observations by appViewModel.observations.collectAsState()

    val defect = defects.find { it.defectId == defectId } ?: return
    val defectObservations = observations.filter { it.defectId == defectId }.sortedBy { it.observedAt }
    val best = defectObservations.maxByOrNull { it.confidence }
    val severity = best?.let { Severity.fromConfidence(it.confidence) } ?: Severity.MODERATE

    Column(Modifier.fillMaxSize().background(Color(0xFFFBFBFA))) {
        DetailTopBar(title = "Defect details", onBack = onBack)

        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            item {
                Spacer(Modifier.height(16.dp))
                // Evidence hero
                if (best != null) {
                    EvidenceImage(
                        relativePath = best.imageRelativePath,
                        loader = appViewModel::loadEvidence,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(16.dp))
                    )
                } else {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(16.dp)).background(SurfaceHigh),
                        contentAlignment = Alignment.Center
                    ) { Text("No evidence image", style = BodyMd, color = Slate) }
                }

                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(defect.defectId, style = HeadlineSm, color = Ink)
                    SeverityChip(severity)
                }
                Text(
                    "Status: ${defect.status.replaceFirstChar { it.uppercase() }} · ${defect.observationCount} observation(s)",
                    style = LabelSm, color = Slate
                )

                Spacer(Modifier.height(16.dp))

                // Metadata card
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface).padding(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DetailRow("Location", GeoUtils.formatCoordinate(defect.latestLatitude, defect.latestLongitude), Icons.Filled.LocationOn)
                        DetailRow("GPS accuracy", GeoUtils.formatAccuracy(defect.latestGpsAccuracy), Icons.Filled.GpsFixed)
                        DetailRow("First observed", TimeUtils.formatDateTime(defect.firstDetectedAt), null)
                        DetailRow("Most recent", TimeUtils.formatDateTime(defect.lastObservedAt), null)
                        best?.let {
                            DetailRow("Best confidence", "${(it.confidence * 100).toInt()}%", null)
                            DetailRow("Location quality", it.locationQuality, null)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text("Observation history", style = LabelLg, color = Ink)
                Spacer(Modifier.height(8.dp))
            }

            items(defectObservations.size) { index ->
                val obs = defectObservations[index]
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp)).background(SurfaceLow).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EvidenceImage(
                        relativePath = obs.imageRelativePath,
                        loader = appViewModel::loadEvidence,
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(TimeUtils.formatDateTime(obs.observedAt), style = LabelMd, color = Ink)
                        Text(
                            "${(obs.confidence * 100).toInt()}% confidence · ${obs.locationQuality}",
                            style = LabelSm, color = Slate
                        )
                        if (obs.latitude != null && obs.longitude != null) {
                            Text(GeoUtils.formatCoordinate(obs.latitude, obs.longitude), style = LabelSm, color = Slate)
                        } else {
                            Text("Location unavailable", style = LabelSm, color = Muted)
                        }
                    }
                    Text("${(obs.confidence * 100).toInt()}%", style = LabelLg.copy(fontWeight = FontWeight.SemiBold), color = Primary)
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp)).background(Primary)
                        .clickable { onOpenMap() },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Map, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("View on map", style = LabelLg, color = Color.White)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Muted, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, style = BodySm, color = Slate, modifier = Modifier.weight(1f))
        Text(value, style = LabelMd, color = Ink, modifier = Modifier.weight(1.4f))
    }
}
