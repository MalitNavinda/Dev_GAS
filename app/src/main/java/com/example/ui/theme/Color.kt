package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Civic Utility Light Tokens
val CivicNavyLight = Color(0xFF0B3C5D)
val OnCivicNavyLight = Color(0xFFFFFFFF)
val CivicNavyContainerLight = Color(0xFFD4E6F6)
val OnCivicNavyContainerLight = Color(0xFF051E31)

val LankaTealLight = Color(0xFF0E7C7B)
val OnLankaTealLight = Color(0xFFFFFFFF)
val LankaTealContainerLight = Color(0xFFCCF2F1)
val OnLankaTealContainerLight = Color(0xFF002020)

val WarmAlabasterBg = Color(0xFFF5F7FA)
val PureSurfaceLight = Color(0xFFFFFFFF)
val SurfaceContainerLight = Color(0xFFEBEEF3)
val SurfaceContainerHighLight = Color(0xFFE1E6EE)
val OnSurfaceInkLight = Color(0xFF101820)
val OnSurfaceVariantLight = Color(0xFF425060)
val OutlineCrispLight = Color(0xFF728193)

// Civic Utility Dark Tokens
val CivicNavyDark = Color(0xFF7CC4FA)
val OnCivicNavyDark = Color(0xFF003352)
val CivicNavyContainerDark = Color(0xFF0B4A73)
val OnCivicNavyContainerDark = Color(0xFFD4E6F6)

val LankaTealDark = Color(0xFF56DBD9)
val OnLankaTealDark = Color(0xFF003737)
val LankaTealContainerDark = Color(0xFF00504F)
val OnLankaTealContainerDark = Color(0xFFCCF2F1)

val DeepGraphiteBg = Color(0xFF0D131A)
val SurfaceDark = Color(0xFF141C25)
val SurfaceContainerDark = Color(0xFF1C2632)
val SurfaceContainerHighDark = Color(0xFF253241)
val OnSurfaceDark = Color(0xFFEAF0F6)
val OnSurfaceVariantDark = Color(0xFFAAB7C6)
val OutlineDark = Color(0xFF607082)

@Immutable
data class LankaGasExtendedColors(
    val litroBlue: Color,
    val onLitroBlue: Color,
    val litroContainer: Color,
    val onLitroContainer: Color,
    val laugfsYellow: Color,
    val onLaugfsYellow: Color,
    val laugfsContainer: Color,
    val onLaugfsContainer: Color,
    val statusAvailable: Color,
    val statusAvailableContainer: Color,
    val onStatusAvailableContainer: Color,
    val statusTransit: Color,
    val statusTransitContainer: Color,
    val onStatusTransitContainer: Color,
    val statusScheduled: Color,
    val statusScheduledContainer: Color,
    val onStatusScheduledContainer: Color,
    val statusSoldOut: Color,
    val statusSoldOutContainer: Color,
    val onStatusSoldOutContainer: Color,
    val mapOceanSurface: Color,
    val mapLandFill: Color,
    val mapLandStroke: Color,
    val mapGridLine: Color
)

val LightExtendedColors = LankaGasExtendedColors(
    litroBlue = Color(0xFF0057B8),
    onLitroBlue = Color(0xFFFFFFFF),
    litroContainer = Color(0xFFD9E8FF),
    onLitroContainer = Color(0xFF002759),
    laugfsYellow = Color(0xFFF5B800),
    onLaugfsYellow = Color(0xFF161300),
    laugfsContainer = Color(0xFFFFF0B8),
    onLaugfsContainer = Color(0xFF3D2C00),
    statusAvailable = Color(0xFF15803D),
    statusAvailableContainer = Color(0xFFDCFCE7),
    onStatusAvailableContainer = Color(0xFF064E23),
    statusTransit = Color(0xFFD97706),
    statusTransitContainer = Color(0xFFFEF3C7),
    onStatusTransitContainer = Color(0xFF6B3400),
    statusScheduled = Color(0xFF475569),
    statusScheduledContainer = Color(0xFFE2E8F0),
    onStatusScheduledContainer = Color(0xFF1E293B),
    statusSoldOut = Color(0xFFB91C1C),
    statusSoldOutContainer = Color(0xFFFEE2E2),
    onStatusSoldOutContainer = Color(0xFF690B0B),
    mapOceanSurface = Color(0xFFE3EEF7),
    mapLandFill = Color(0xFFFDFEFE),
    mapLandStroke = Color(0xFF8AA4BC),
    mapGridLine = Color(0x220B3C5D)
)

val DarkExtendedColors = LankaGasExtendedColors(
    litroBlue = Color(0xFF3B8EF3),
    onLitroBlue = Color(0xFF03152D),
    litroContainer = Color(0xFF0B356B),
    onLitroContainer = Color(0xFFD9E8FF),
    laugfsYellow = Color(0xFFFFCC29),
    onLaugfsYellow = Color(0xFF1F1700),
    laugfsContainer = Color(0xFF574000),
    onLaugfsContainer = Color(0xFFFFF0B8),
    statusAvailable = Color(0xFF4ADE80),
    statusAvailableContainer = Color(0xFF0B3F21),
    onStatusAvailableContainer = Color(0xFFBBF7D0),
    statusTransit = Color(0xFFFBBF24),
    statusTransitContainer = Color(0xFF4F2B04),
    onStatusTransitContainer = Color(0xFFFDE68A),
    statusScheduled = Color(0xFF94A3B8),
    statusScheduledContainer = Color(0xFF273447),
    onStatusScheduledContainer = Color(0xFFE2E8F0),
    statusSoldOut = Color(0xFFF87171),
    statusSoldOutContainer = Color(0xFF4E1212),
    onStatusSoldOutContainer = Color(0xFFFECACA),
    mapOceanSurface = Color(0xFF0F1B27),
    mapLandFill = Color(0xFF1B2B3A),
    mapLandStroke = Color(0xFF476987),
    mapGridLine = Color(0x267CC4FA)
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
