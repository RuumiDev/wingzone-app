package wingzone.zenith.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// WingZone Branding Colors
val WingZoneRed = Color(0xFF820000)
val WingZoneRedLight = Color(0xFFb3282e)
val WingZoneOrange = Color(0xFFef7725)

// Accent Colors
val DarkGray = Color(0xFF333333)
val LightGray = Color(0xFFe1e0df)

// Neutral Colors - Light Mode
val BackgroundGray = Color(0xFFF5F5F5)
val CardWhite = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF2C3E50)
val TextSecondary = Color(0xFF7F8C8D)

// Neutral Colors - Dark Mode
val TextPrimaryDark = Color(0xFFE8E8E8)
val TextSecondaryDark = Color(0xFFB0B0B0)
val CardWhiteDark = Color(0xFF2C2C2C)

/**
 * Get adaptive text colors - FORCED to light mode for WingZone branding
 * (App uses forced light theme, ignoring system dark mode)
 */
@Composable
fun getAdaptiveTextPrimary(): Color {
    return TextPrimary  // Always use light mode text
}

@Composable
fun getAdaptiveTextSecondary(): Color {
    return TextSecondary  // Always use light mode text
}

@Composable
fun getAdaptiveCardBackground(): Color {
    return CardWhite  // Always use light mode background
}
