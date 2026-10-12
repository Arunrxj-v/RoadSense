package com.roadsensevision.ui.theme

import androidx.compose.ui.graphics.Color

// RoadSense Vision design system (light mode only).
// Reference: Stitch "Road Utility & Detection System" DESIGN.md

// Canvas & surfaces
val CanvasBackground = Color(0xFFFBFBFA)   // warm off-white canvas
val Surface = Color(0xFFFFFFFF)            // elevated card surface
val SurfaceLow = Color(0xFFF3F4F6)         // subtle fill / grouped control container
val SurfaceHigh = Color(0xFFE5E7EB)        // hairline borders / dividers
val Hairline = Color(0xFFE5E7EB)           // 1px structural border
val InputBorder = Color(0xFFD1D5DB)        // form field border

// Brand accents
val Primary = Color(0xFFEA580C)            // signal orange — pothole boxes, primary actions, pins
val PrimaryDark = Color(0xFFC2410C)        // pressed/hover
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryTint = Color(0xFFFFEDD5)        // severity chip tint

// Positive / status
val Secondary = Color(0xFF16A34A)          // green status
val SecondaryTint = Color(0xFFDCFCE7)
val OnSecondaryTint = Color(0xFF15803D)

// Neutral text
val Ink = Color(0xFF111827)              // headings, primary values
val Slate = Color(0xFF4B5563)            // metadata, helper text
val Muted = Color(0xFF9CA3AF)            // placeholders, tertiary

// Camera HUD overlay (from design inverse-surface)
val HudOverlay = Color(0xFF141B2B)
val OnHudOverlay = Color(0xFFEDF0FF)

// Error
val Error = Color(0xFFDC2626)
val ErrorTint = Color(0xFFFEE2E2)

// Filter chip selected
val FilterSelected = Color(0xFF111827)
