package com.roadsensevision.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roadsensevision.ui.theme.Inter
import com.roadsensevision.ui.theme.LabelSm
import com.roadsensevision.ui.theme.Muted
import com.roadsensevision.ui.theme.Primary
import com.roadsensevision.ui.theme.Slate
import com.roadsensevision.ui.theme.Surface

/** Bottom navigation destinations (design: Home, Monitor, Map, History). */
enum class NavTab(val label: String, val icon: ImageVector, val route: String) {
    HOME("Home", Icons.Filled.Home, "home"),
    MONITOR("Monitor", Icons.Filled.Videocam, "monitor"),
    MAP("Map", Icons.Filled.Map, "map"),
    HISTORY("History", Icons.Filled.History, "history")
}

@Composable
fun RoadSenseBottomBar(
    currentRoute: String?,
    onNavigate: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Surface)
    ) {
        Spacer(Modifier.height(0.5.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTab.entries.forEach { tab ->
                val selected = tab.route == currentRoute
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigate(tab) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (selected) Primary else Muted,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tab.label,
                        style = LabelSm.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = if (selected) Primary else Slate
                    )
                }
            }
        }
    }
}
