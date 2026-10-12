package com.roadsensevision.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

private val RoadSenseColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = Secondary,
    onSecondary = Color.White,
    background = CanvasBackground,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = SurfaceLow,
    onSurfaceVariant = Slate,
    outline = Hairline,
    error = Error
)

private val RoadSenseTypography = Typography(
    displayLarge = HeadlineLg,
    headlineLarge = HeadlineLg,
    headlineMedium = HeadlineMd,
    headlineSmall = HeadlineSm,
    titleLarge = HeadlineSm,
    titleMedium = LabelLg,
    titleSmall = LabelMd,
    bodyLarge = BodyLg,
    bodyMedium = BodyMd,
    bodySmall = BodySm,
    labelLarge = LabelLg,
    labelMedium = LabelMd,
    labelSmall = LabelSm
)

@Composable
fun RoadSenseVisionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RoadSenseColorScheme,
        typography = RoadSenseTypography,
        content = content
    )
}

// Shared font family accessor for TextStyles built ad hoc.
val RoadSenseFontFamily: FontFamily = Inter
