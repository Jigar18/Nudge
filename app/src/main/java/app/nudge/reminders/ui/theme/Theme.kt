package app.nudge.reminders.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.nudge.reminders.R
import app.nudge.reminders.data.Priority

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B47E0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E2FF),
    onPrimaryContainer = Color(0xFF1F1468),
    secondary = Color(0xFFF2643F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE6DE),
    onSecondaryContainer = Color(0xFF6A2210),
    tertiary = Color(0xFF14A877),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD5F5E8),
    onTertiaryContainer = Color(0xFF00382A),
    background = Color(0xFFF6F5FB),
    onBackground = Color(0xFF17151F),
    surface = Color(0xFFF6F5FB),
    onSurface = Color(0xFF17151F),
    surfaceVariant = Color(0xFFECE9F5),
    onSurfaceVariant = Color(0xFF68647C),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF0EEF8),
    surfaceContainerHigh = Color(0xFFEAE7F4),
    surfaceContainerHighest = Color(0xFFE3DFF0),
    outline = Color(0xFFCFCADF),
    outlineVariant = Color(0xFFE6E2F0),
    error = Color(0xFFE5484D),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFADA1FF),
    onPrimary = Color(0xFF22157A),
    primaryContainer = Color(0xFF3B2DB0),
    onPrimaryContainer = Color(0xFFE7E2FF),
    secondary = Color(0xFFFF9576),
    onSecondary = Color(0xFF4A1405),
    secondaryContainer = Color(0xFF4F2216),
    onSecondaryContainer = Color(0xFFFFDCD1),
    tertiary = Color(0xFF4ADE9F),
    onTertiary = Color(0xFF003827),
    tertiaryContainer = Color(0xFF0F4433),
    onTertiaryContainer = Color(0xFFC6F7E3),
    background = Color(0xFF0E0D14),
    onBackground = Color(0xFFECEAF4),
    surface = Color(0xFF0E0D14),
    onSurface = Color(0xFFECEAF4),
    surfaceVariant = Color(0xFF221F2E),
    onSurfaceVariant = Color(0xFFA6A1BA),
    surfaceContainerLowest = Color(0xFF0A090F),
    surfaceContainerLow = Color(0xFF17151F),
    surfaceContainer = Color(0xFF1B1925),
    surfaceContainerHigh = Color(0xFF23202F),
    surfaceContainerHighest = Color(0xFF2B2839),
    outline = Color(0xFF3B3749),
    outlineVariant = Color(0xFF2A2736),
    error = Color(0xFFFF6B6F),
)

val Jakarta = FontFamily(
    Font(R.font.jakarta_regular, FontWeight.Normal),
    Font(R.font.jakarta_medium, FontWeight.Medium),
    Font(R.font.jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.jakarta_bold, FontWeight.Bold),
    Font(R.font.jakarta_extrabold, FontWeight.ExtraBold),
)

private val NudgeTypography = Typography().run {
    fun TextStyle.jakarta(weight: FontWeight? = null) =
        copy(fontFamily = Jakarta, fontWeight = weight ?: fontWeight)
    Typography(
        displaySmall = displaySmall.jakarta(FontWeight.ExtraBold),
        headlineLarge = headlineLarge.jakarta(FontWeight.ExtraBold),
        headlineMedium = headlineMedium.jakarta(FontWeight.ExtraBold).copy(letterSpacing = (-0.5).sp),
        headlineSmall = headlineSmall.jakarta(FontWeight.Bold),
        titleLarge = titleLarge.jakarta(FontWeight.Bold),
        titleMedium = titleMedium.jakarta(FontWeight.SemiBold),
        titleSmall = titleSmall.jakarta(FontWeight.SemiBold),
        bodyLarge = bodyLarge.jakarta(),
        bodyMedium = bodyMedium.jakarta(),
        bodySmall = bodySmall.jakarta(),
        labelLarge = labelLarge.jakarta(FontWeight.SemiBold),
        labelMedium = labelMedium.jakarta(FontWeight.SemiBold),
        labelSmall = labelSmall.jakarta(FontWeight.Bold),
    )
}

/** Signature gradient shared with the launcher icon. */
val BrandGradient = Brush.linearGradient(listOf(Color(0xFF8A7BFF), Color(0xFF5B47E0), Color(0xFF34209A)))

@Composable
fun Priority.color(): Color {
    val dark = isSystemInDarkTheme()
    return when (this) {
        Priority.LOW -> if (dark) Color(0xFF4ADE9F) else Color(0xFF16A874)
        Priority.MEDIUM -> if (dark) Color(0xFFFFC25C) else Color(0xFFF0A01C)
        Priority.HIGH -> if (dark) Color(0xFFFF7A85) else Color(0xFFF04E5E)
    }
}

@Composable
fun NudgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = NudgeTypography,
        content = content,
    )
}
