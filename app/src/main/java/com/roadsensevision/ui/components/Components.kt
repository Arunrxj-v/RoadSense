package com.roadsensevision.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadsensevision.domain.models.Severity
import com.roadsensevision.ui.theme.HeadlineLg
import com.roadsensevision.ui.theme.HeadlineSm
import com.roadsensevision.ui.theme.LabelLg
import com.roadsensevision.ui.theme.LabelMd
import com.roadsensevision.ui.theme.LabelSm
import com.roadsensevision.ui.theme.OnSecondaryTint
import com.roadsensevision.ui.theme.Primary
import com.roadsensevision.ui.theme.PrimaryDark
import com.roadsensevision.ui.theme.PrimaryTint
import com.roadsensevision.ui.theme.Secondary
import com.roadsensevision.ui.theme.SecondaryTint
import com.roadsensevision.ui.theme.Slate
import com.roadsensevision.ui.theme.Surface
import com.roadsensevision.ui.theme.SurfaceLow
import com.roadsensevision.ui.theme.SurfaceHigh
import com.roadsensevision.ui.theme.Ink
import com.roadsensevision.ui.theme.BodyMd

/** Pill status badge with a leading dot (online / operational). */
@Composable
fun StatusChip(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val (bg, fg, dot) = if (active) Triple(SecondaryTint, OnSecondaryTint, Secondary)
    else Triple(SurfaceLow, Slate, MaterialTheme.colorScheme.outline)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(6.dp))
        Text(label, style = LabelMd, color = fg)
    }
}

/** Severity pill (confidence-based estimate). */
@Composable
fun SeverityChip(severity: Severity, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(PrimaryTint)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(severity.label, style = LabelSm, color = PrimaryDark)
    }
}

/** Solid signal-orange primary action. */
@Composable
fun PrimaryActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Primary)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        icon?.invoke()
        Spacer(Modifier.width(8.dp))
        Text(label, style = LabelLg, color = Color.White)
    }
}

/** White bordered secondary action. */
@Composable
fun SecondaryActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Surface)
            .border(1.dp, SurfaceHigh, RoundedCornerShape(8.dp))
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        icon?.invoke()
        Spacer(Modifier.width(8.dp))
        Text(label, style = LabelLg, color = Ink)
    }
}

/** Editorial section heading. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(title, style = LabelLg, color = Ink, modifier = modifier)
}

/** A 3-up metric tile used on the dashboard and session summary. */
@Composable
fun MetricTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueStyle: androidx.compose.ui.text.TextStyle = HeadlineLg
) {
    Column(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = valueStyle, color = Ink)
        Spacer(Modifier.height(2.dp))
        Text(label, style = LabelSm, color = Slate)
    }
}

/** Proper empty state for zero-data screens. */
@Composable
fun EmptyState(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(SurfaceLow),
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(Modifier.height(16.dp))
        Text(title, style = HeadlineSm, color = Ink)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = BodyMd,
            color = Slate,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
