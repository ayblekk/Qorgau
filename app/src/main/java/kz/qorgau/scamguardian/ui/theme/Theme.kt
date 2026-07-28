package kz.qorgau.scamguardian.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = GuardianBlue,
    onPrimary = Color.White,
    secondary = SoftTeal,
    onSecondary = Color.White,
    tertiary = WarningAmber,
    error = AlertCoral,
    onError = Color.White,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
)

private val DarkColors = darkColorScheme(
    primary = SoftTeal,
    onPrimary = Color.White,
    secondary = SoftTeal,
    onSecondary = Color.White,
    tertiary = WarningAmber,
    error = AlertCoral,
    onError = Color.White,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryLight,
)

@Composable
fun ScamGuardianTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Dynamic color intentionally unused — fixed brand palette (DESIGN.md).
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ScamGuardianTypography,
        content = content,
    )
}
