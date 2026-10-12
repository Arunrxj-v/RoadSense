package com.roadsensevision

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roadsensevision.monitoring.MonitoringViewModel
import com.roadsensevision.navigation.RoadSenseNavGraph
import com.roadsensevision.ui.AppViewModel
import com.roadsensevision.ui.theme.RoadSenseVisionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoadSenseVisionTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val appViewModel: AppViewModel = viewModel()
                    val monitoringViewModel: MonitoringViewModel = viewModel()
                    RoadSenseNavGraph(
                        appViewModel = appViewModel,
                        monitoringViewModel = monitoringViewModel
                    )
                }
            }
        }
    }
}
