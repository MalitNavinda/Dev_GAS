package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.ui.i18n.LankaGasTranslations
import com.example.ui.i18n.LocalStrings

private val LightColorScheme = lightColorScheme(
    primary = CivicNavyLight,
    onPrimary = OnCivicNavyLight,
    primaryContainer = CivicNavyContainerLight,
    onPrimaryContainer = OnCivicNavyContainerLight,
    secondary = LankaTealLight,
    onSecondary = OnLankaTealLight,
    secondaryContainer = LankaTealContainerLight,
    onSecondaryContainer = OnLankaTealContainerLight,
    tertiary = ColorTokensBridge.AmberLight,
    onTertiary = OnCivicNavyLight,
    background = WarmAlabasterBg,
    onBackground = OnSurfaceInkLight,
    surface = PureSurfaceLight,
    onSurface = OnSurfaceInkLight,
    surfaceVariant = SurfaceContainerLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    outline = OutlineCrispLight
)

private val DarkColorScheme = darkColorScheme(
    primary = CivicNavyDark,
    onPrimary = OnCivicNavyDark,
    primaryContainer = CivicNavyContainerDark,
    onPrimaryContainer = OnCivicNavyContainerDark,
    secondary = LankaTealDark,
    onSecondary = OnLankaTealDark,
    secondaryContainer = LankaTealContainerDark,
    onSecondaryContainer = OnLankaTealContainerDark,
    tertiary = ColorTokensBridge.AmberDark,
    onTertiary = OnCivicNavyDark,
    background = DeepGraphiteBg,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceContainerDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    outline = OutlineDark
)

private object ColorTokensBridge {
    val AmberLight = LightExtendedColors.statusTransit
    val AmberDark = DarkExtendedColors.statusTransit
}

val LankaGasShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun LankaGasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    language: AppLanguage = AppLanguage.EN,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    val strings = LankaGasTranslations.forLanguage(language)

    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors,
        LocalStrings provides strings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = LankaGasShapes,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    LankaGasTheme(darkTheme = darkTheme, content = content)
}
