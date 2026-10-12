package com.roadsensevision.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddRoad
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.roadsensevision.ui.theme.HeadlineSm
import com.roadsensevision.ui.theme.Ink
import com.roadsensevision.ui.theme.OnPrimary
import com.roadsensevision.ui.theme.Primary
import com.roadsensevision.ui.theme.Surface

/** Brand logo tile (signal-orange square with road glyph). */
@Composable
fun RoadSenseLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Primary),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.AddRoad, contentDescription = "RoadSense logo", tint = OnPrimary, modifier = Modifier.size(22.dp))
    }
}

/** Home top app bar: logo, title, settings, profile. */
@Composable
fun HomeTopBar(onOpenSettings: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Surface)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoadSenseLogo()
        Spacer(Modifier.width(8.dp))
        Text("RoadSense", style = HeadlineSm, color = Ink, modifier = Modifier.weight(1f))
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(50)).clickable { onOpenSettings() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Ink)
        }
        Spacer(Modifier.width(4.dp))
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(Primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Person, contentDescription = "Profile", tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

/** Stack top app bar with back button (detail screens). */
@Composable
fun DetailTopBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Surface)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(50)).clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink)
        }
        Spacer(Modifier.width(4.dp))
        Text(title, style = HeadlineSm, color = Ink)
    }
}
