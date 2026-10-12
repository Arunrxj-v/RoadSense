package com.roadsensevision.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.roadsensevision.camera.toUprightBitmap
import com.roadsensevision.monitoring.MonitoringViewModel
import com.roadsensevision.ui.components.DetectionOverlay
import com.roadsensevision.ui.components.ImageTransform
import com.roadsensevision.ui.theme.HeadlineSm
import com.roadsensevision.ui.theme.Ink
import com.roadsensevision.ui.theme.LabelLg
import com.roadsensevision.ui.theme.LabelMd
import com.roadsensevision.ui.theme.LabelSm
import com.roadsensevision.ui.theme.Muted
import com.roadsensevision.ui.theme.OnHudOverlay
import com.roadsensevision.ui.theme.Primary
import com.roadsensevision.ui.theme.Secondary
import com.roadsensevision.ui.theme.SecondaryTint
import com.roadsensevision.ui.theme.Slate
import com.roadsensevision.ui.theme.Surface
import com.roadsensevision.ui.theme.SurfaceHigh
import com.roadsensevision.ui.theme.SurfaceLow
import com.roadsensevision.utilities.TimeUtils
import java.util.concurrent.Executors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

@Composable
fun LiveMonitoringScreen(
    monitoringViewModel: MonitoringViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by monitoringViewModel.ui.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasCameraPermission = results[Manifest.permission.CAMERA] == true
        hasLocationPermission = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        val need = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
        ).filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (need.isNotEmpty()) permissionLauncher.launch(need.toTypedArray())
    }

    // Auto-start a session on entry only after camera is ready and model is loaded.
    // We wait for the camera permission + setup LaunchedEffect to complete first.
    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission && !monitoringViewModel.ui.value.sessionActive) {
            monitoringViewModel.startMonitoring()
        }
    }

    // GPS status is refreshed automatically via the UI state collector below;
    // no manual polling loop is needed.

    val inferenceExecutor = remember { Executors.newSingleThreadExecutor() }
    val cameraController = remember { LifecycleCameraController(context) }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    // GPS refresh: update UI state once per second while monitoring.
    LaunchedEffect(monitoringViewModel.ui.value.sessionActive) {
        if (monitoringViewModel.ui.value.sessionActive) {
            launch {
                while (monitoringViewModel.ui.value.sessionActive) {
                    monitoringViewModel.refreshGpsStatus()
                    delay(1000)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraController.clearImageAnalysisAnalyzer()
            cameraController.unbind()
            inferenceExecutor.shutdown()
            monitoringViewModel.stopMonitoring()
        }
    }

    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission) {
            try {
                cameraController.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraController.imageAnalysisOutputImageFormat = ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
                cameraController.imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                cameraController.setImageAnalysisAnalyzer(inferenceExecutor) { imageProxy ->
                    try {
                        val upright = imageProxy.toUprightBitmap()
                        if (upright != null) {
                            val now = System.currentTimeMillis()
                            monitoringViewModel.onFrame(upright, upright.width, upright.height, now)
                            if (!upright.isRecycled) upright.recycle()
                        }
                    } finally {
                        imageProxy.close()
                    }
                }
                cameraController.bindToLifecycle(lifecycleOwner)
                previewView.controller = cameraController
            } catch (e: Exception) {
                android.util.Log.e("RoadSense", "Camera init failed", e)
            }
        }
    }

    // ---- Overlay transform ----
    var previewSize by remember { mutableStateOf(IntSize.Zero) }
    val transform = if (previewSize.width > 0 && state.imageWidth > 0) {
        ImageTransform.fillCenter(
            imageW = state.imageWidth,
            imageH = state.imageHeight,
            viewW = previewSize.width.toFloat(),
            viewH = previewSize.height.toFloat()
        )
    } else ImageTransform.Identity

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFFBFBFA))
    ) {
        // Top bar
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Surface)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Ink)
            }
            Text("Live Monitoring", style = HeadlineSm, color = Ink, modifier = Modifier.weight(1f))
        }

        // Camera viewport
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .onGloballyPositioned { coords ->
                    previewSize = coords.size
                }
                .background(Color.Black)
        ) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            // Detection overlay
            DetectionOverlay(
                detections = state.detections,
                transform = transform,
                modifier = Modifier.fillMaxSize()
            )

            // HUD top row
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Live + elapsed
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(colorWithAlpha(com.roadsensevision.ui.theme.HudOverlay, 0.85f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(if (state.isPaused) Muted else Secondary))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (state.isPaused) "PAUSED" else "LIVE",
                        style = LabelSm.copy(fontWeight = FontWeight.SemiBold),
                        color = OnHudOverlay
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(TimeUtils.formatDuration(state.durationMs), style = LabelSm, color = OnHudOverlay)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // GPS status
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(colorWithAlpha(com.roadsensevision.ui.theme.HudOverlay, 0.85f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = null, tint = SecondaryTint, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            when {
                                state.gpsState.name == "ACTIVE" -> "GPS Active ${com.roadsensevision.utilities.GeoUtils.formatAccuracy(state.gpsAccuracy)}"
                                state.gpsState.name == "SEARCHING" -> "GPS searching"
                                else -> "GPS unavailable"
                            },
                            style = LabelSm,
                            color = OnHudOverlay
                        )
                    }
                    // Detected count
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Primary)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${state.confirmedCount} detected", style = LabelSm, color = Color.White)
                    }
                }
            }
        }

        // Bottom control sheet
        Column(
            Modifier
                .fillMaxWidth()
                .background(Surface)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Drag handle
            Box(
                Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(SurfaceHigh)
            )
            Spacer(Modifier.height(12.dp))

            // Telemetry row
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceLow)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryCell("GPS accuracy", com.roadsensevision.utilities.GeoUtils.formatAccuracy(state.gpsAccuracy), Modifier.weight(1f))
                TelemetryCell("Model", "On-device YOLO", Modifier.weight(1f))
                TelemetryCell(
                    "Inference",
                    if (state.inferenceFps > 0f) String.format(java.util.Locale.US, "%.1f FPS (%.0fms)", state.inferenceFps, state.inferenceLatencyMs)
                    else "—",
                    Modifier.weight(1.3f)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Action bar
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceHigh)
                        .clickable {
                            if (state.isPaused) monitoringViewModel.resumeMonitoring()
                            else monitoringViewModel.pauseMonitoring()
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (state.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.isPaused) "Resume" else "Pause", style = LabelLg, color = Ink)
                }
                Row(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Primary)
                        .clickable {
                            monitoringViewModel.stopMonitoring()
                            onBack()
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("End session", style = LabelLg, color = Color.White)
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(Secondary))
                Spacer(Modifier.width(6.dp))
                Text("Geo-tagged recordings saved automatically", style = LabelSm, color = Slate)
            }
        }
    }
}

@Composable
private fun TelemetryCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = LabelSm, color = Slate)
        Spacer(Modifier.height(2.dp))
        Text(value, style = LabelMd.copy(fontWeight = FontWeight.SemiBold), color = Ink, maxLines = 1)
    }
}

private fun colorWithAlpha(color: Color, alpha: Float): Color = color.copy(alpha = alpha)
