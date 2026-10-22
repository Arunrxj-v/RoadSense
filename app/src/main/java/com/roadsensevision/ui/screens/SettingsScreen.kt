package com.roadsensevision.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.roadsensevision.inference.ModelInfo
import com.roadsensevision.monitoring.MonitoringViewModel
import com.roadsensevision.monitoring.ModelStatus
import com.roadsensevision.ui.AppViewModel
import com.roadsensevision.ui.components.DetailTopBar
import com.roadsensevision.ui.components.StatusChip
import com.roadsensevision.ui.theme.BodyMd
import com.roadsensevision.ui.theme.BodySm
import com.roadsensevision.ui.theme.HeadlineLg
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
import com.roadsensevision.ui.theme.Secondary

@Composable
fun SettingsScreen(
    appViewModel: AppViewModel,
    monitoringViewModel: MonitoringViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by appViewModel.settings.collectAsState()
    val syncSummary by appViewModel.syncSummary.collectAsState()
    val monitoringState by monitoringViewModel.ui.collectAsState()

    val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    val locationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

    val modelReady = monitoringState.modelStatus == ModelStatus.READY
    val evidenceBytes = appViewModel.evidenceStorageBytes()
    val syncDescription = when {
        !syncSummary.configured -> "Ingestion API key is not configured"
        syncSummary.uploading -> "Uploading queued observations"
        syncSummary.failed > 0 -> "${syncSummary.failed} failed · ${syncSummary.pending} pending"
        syncSummary.pending > 0 -> "${syncSummary.pending} observations queued for upload"
        syncSummary.noLocation > 0 -> "${syncSummary.synced} synced · ${syncSummary.noLocation} without GPS"
        else -> "${syncSummary.synced} observations synced"
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFFBFBFA))) {
        DetailTopBar(title = "Settings", onBack = onBack)

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Settings", style = HeadlineLg, color = Ink)
                    Text("Device permissions & prototype configuration", style = BodyMd, color = Slate)
                }
                StatusChip(label = if (modelReady) "Ready" else "Loading", active = modelReady)
            }

            Spacer(Modifier.height(20.dp))

            // ---- Permissions ----
            SectionLabel("Permissions & hardware access")
            CardColumn {
                PermissionRow(Icons.Filled.Camera, "Camera sensor", "Used for live pothole detection", cameraGranted)
                Divider()
                PermissionRow(Icons.Filled.LocationOn, "Location & GPS", "Associates detections with coordinates", locationGranted)
            }

            Spacer(Modifier.height(20.dp))

            // ---- AI & Detection Engine ----
            SectionLabel("AI & detection engine")
            CardColumn {
                InfoRow(Icons.Filled.Memory, "Detection model",
                    "best.onnx · ${ModelInfo.IMAGE_SIZE} · ${ModelInfo.CLASS_LABEL}",
                    trailing = "CPU")
                Divider()
                InfoRow(Icons.Filled.Memory, "Input / output",
                    "In ${ModelInfo.INPUT_SHAPE} → Out ${ModelInfo.OUTPUT_SHAPE}",
                    trailing = "FP32")
                Divider()
                InfoRow(Icons.Filled.Memory, "Inference",
                    "${ModelInfo.INFERENCE_PROVIDER} · ${ModelInfo.OPSET}", trailing = "")

                Divider()
                // Confidence threshold slider
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Confidence threshold", style = HeadlineSm, color = Ink, modifier = Modifier.weight(1f))
                        Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Primary).padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text(String.format(java.util.Locale.US, "%.2f", settings.confidenceThreshold),
                                style = LabelMd.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        }
                    }
                    Text("Field accuracy target", style = BodySm, color = Slate)
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = settings.confidenceThreshold,
                        onValueChange = { appViewModel.updateConfidence(it) },
                        valueRange = 0.50f..0.95f,
                        steps = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("0.50 (High sensitivity)", style = LabelSm, color = Slate)
                        Text("0.95 (Strict)", style = LabelSm, color = Slate)
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("Inference threads", style = LabelLg, color = Ink)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 4).forEach { t ->
                            val selected = settings.inferenceThreads == t
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) Primary else SurfaceLow)
                                    .clickable { appViewModel.updateInferenceThreads(t) }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("$t", style = LabelMd, color = if (selected) Color.White else Slate)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ---- Data & Evidence ----
            SectionLabel("Data & evidence storage")
            CardColumn {
                ToggleRow(
                    Icons.Filled.PhotoCamera, "Auto evidence capture",
                    "Save a frame when a pothole is confirmed",
                    checked = settings.saveEvidence,
                    onCheckedChange = { appViewModel.updateSaveEvidence(it) }
                )
                Divider()
                InfoRow(
                    Icons.Filled.Save,
                    "Offline-first mode",
                    "Saved locally first, uploaded when connected",
                    trailing = "On"
                )
                Divider()
                // Storage usage
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Evidence storage", style = HeadlineSm, color = Ink, modifier = Modifier.weight(1f))
                        Text(formatBytes(evidenceBytes), style = LabelMd.copy(fontWeight = FontWeight.SemiBold), color = Ink)
                    }
                    val total = formatBytes(evidenceBytes)
                    Text("Images stored in app-private storage · $total used", style = BodySm, color = Slate)
                }
            }

            Spacer(Modifier.height(20.dp))

            // ---- Server sync ----
            SectionLabel("Server sync")
            CardColumn {
                InfoRow(
                    Icons.Filled.Sync,
                    "Observation upload",
                    syncDescription,
                    trailing = when {
                        !syncSummary.configured -> "Off"
                        syncSummary.uploading -> "Uploading"
                        else -> "On"
                    }
                )
                Divider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = syncSummary.configured && !syncSummary.uploading) {
                            appViewModel.syncNow()
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Sync, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Sync now", style = HeadlineSm, color = Ink)
                        Text("Retry queued and failed observations", style = BodySm, color = Slate)
                    }
                    Text(
                        when {
                            syncSummary.uploading -> "Uploading"
                            syncSummary.pending + syncSummary.failed > 0 -> "Retry"
                            syncSummary.noLocation > 0 -> "GPS needed"
                            else -> "Up to date"
                        },
                        style = LabelMd,
                        color = Primary
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ---- Diagnostics ----
            SectionLabel("Diagnostics & field tools")
            CardColumn {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = com.roadsensevision.ui.theme.Error, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Clear all local data", style = HeadlineSm, color = com.roadsensevision.ui.theme.Error)
                        Text("Removes sessions, defects, observations and images", style = BodySm, color = Slate)
                    }
                    Box(
                        Modifier.clip(RoundedCornerShape(6.dp)).background(com.roadsensevision.ui.theme.ErrorTint).padding(horizontal = 12.dp, vertical = 6.dp)
                            .clickable { appViewModel.clearAllData() }
                    ) {
                        Text("Purge", style = LabelMd, color = com.roadsensevision.ui.theme.Error)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ---- Privacy ----
            SectionLabel("Privacy")
            CardColumn {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Detection and evidence images stay on-device. When the server is configured and reachable, only pothole metadata (coordinates, timestamp, confidence, and GPS accuracy) is uploaded. Camera frames are never sent.",
                        style = BodySm, color = Slate)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Footer
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Secondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("RoadSense Vision v2.0.0", style = LabelMd, color = Ink)
                }
                Text("Phase 2 · on-device detection + server sync", style = LabelSm, color = Slate)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = LabelMd.copy(fontWeight = FontWeight.SemiBold), color = Slate,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
}

@Composable
private fun CardColumn(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface)) {
        content()
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(SurfaceLow).padding(horizontal = 16.dp))
}

@Composable
private fun PermissionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, granted: Boolean) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(SurfaceLow), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = HeadlineSm, color = Ink, maxLines = 1)
            Text(if (granted) subtitle else "Permission required", style = BodySm, color = if (granted) Slate else com.roadsensevision.ui.theme.Error)
        }
        Text(if (granted) "Granted" else "Not granted", style = LabelSm, color = if (granted) Secondary else com.roadsensevision.ui.theme.Error)
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, trailing: String) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(SurfaceLow), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = HeadlineSm, color = Ink, maxLines = 1)
            Text(subtitle, style = BodySm, color = Slate, maxLines = 2)
        }
        if (trailing.isNotEmpty()) {
            Text(trailing, style = LabelSm, color = Slate,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceLow).padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}

@Composable
private fun ToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(SurfaceLow), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = HeadlineSm, color = Ink, maxLines = 1)
            Text(subtitle, style = BodySm, color = Slate, maxLines = 1)
        }
        Box(
            Modifier
                .size(width = 48.dp, height = 26.dp)
                .clip(RoundedCornerShape(50))
                .background(if (checked) Secondary else SurfaceHigh)
                .clickable { onCheckedChange(!checked) },
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                Modifier.padding(horizontal = 3.dp).size(20.dp).clip(CircleShape).background(Color.White)
            )
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024 -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024 -> String.format(java.util.Locale.US, "%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}
