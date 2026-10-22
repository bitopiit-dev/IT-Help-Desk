// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.ithelpdesk.R

// ---------- Type: Sora (OFL, bundled in res/font) ----------

val Sora = FontFamily(
    Font(R.font.sora_regular, FontWeight.Normal),
    Font(R.font.sora_semibold, FontWeight.SemiBold),
    Font(R.font.sora_bold, FontWeight.Bold),
    Font(R.font.sora_extrabold, FontWeight.ExtraBold),
)

private val AppTypography = Typography().let { t ->
    fun TextStyle.sora(weight: FontWeight? = null) = copy(fontFamily = Sora, fontWeight = weight ?: fontWeight)
    t.copy(
        displayLarge = t.displayLarge.sora(FontWeight.ExtraBold),
        displayMedium = t.displayMedium.sora(FontWeight.ExtraBold),
        displaySmall = t.displaySmall.sora(FontWeight.ExtraBold),
        headlineLarge = t.headlineLarge.sora(FontWeight.ExtraBold),
        headlineMedium = t.headlineMedium.sora(FontWeight.Bold),
        headlineSmall = t.headlineSmall.sora(FontWeight.Bold),
        titleLarge = t.titleLarge.sora(FontWeight.Bold),
        titleMedium = t.titleMedium.sora(FontWeight.SemiBold),
        titleSmall = t.titleSmall.sora(FontWeight.SemiBold),
        bodyLarge = t.bodyLarge.sora(),
        bodyMedium = t.bodyMedium.sora(),
        bodySmall = t.bodySmall.sora(),
        labelLarge = t.labelLarge.sora(FontWeight.SemiBold),
        labelMedium = t.labelMedium.sora(FontWeight.SemiBold),
        labelSmall = t.labelSmall.sora(FontWeight.SemiBold),
    )
}

val KpiValueStyle = TextStyle(fontFamily = Sora, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// ---------- Brand: gradients and which app we are in ----------

@Immutable
data class Brand(
    val isAdmin: Boolean,
    val hero: List<Color>,
    val accent: List<Color>,
    val cardTints: List<Color>,
) {
    val heroBrush: Brush get() = Brush.linearGradient(hero)
    val accentBrush: Brush get() = Brush.linearGradient(accent)
}

/** User app: electric violet → hot pink → sunset orange. */
private val UserBrand = Brand(
    isAdmin = false,
    hero = listOf(Color(0xFF7C3AED), Color(0xFFDB2777), Color(0xFFF97316)),
    accent = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)),
    cardTints = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFFF97316), Color(0xFF06B6D4), Color(0xFF22C55E), Color(0xFFEAB308)),
)

/** Admin app: deep-space command centre with neon cyan and lime. */
private val AdminBrand = Brand(
    isAdmin = true,
    hero = listOf(Color(0xFF0EA5E9), Color(0xFF6366F1), Color(0xFFA855F7)),
    accent = listOf(Color(0xFF22D3EE), Color(0xFFA3E635)),
    cardTints = listOf(Color(0xFF22D3EE), Color(0xFFA3E635), Color(0xFFF472B6), Color(0xFFFBBF24), Color(0xFF818CF8), Color(0xFFFB7185)),
)

val LocalBrand = staticCompositionLocalOf { UserBrand }

// ---------- Colour schemes ----------

private val UserLight = lightColorScheme(
    primary = Color(0xFF7C3AED),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE4FF),
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = Color(0xFFDB2777),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0EF),
    onSecondaryContainer = Color(0xFF500724),
    tertiary = Color(0xFFF97316),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE7D3),
    onTertiaryContainer = Color(0xFF431407),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1A1426),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1A1426),
    surfaceVariant = Color(0xFFEFEAF7),
    onSurfaceVariant = Color(0xFF5D5670),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF5F0FC),
    surfaceContainerHigh = Color(0xFFEFE8FA),
    surfaceContainerHighest = Color(0xFFE8E0F5),
    outline = Color(0xFF8E86A3),
    outlineVariant = Color(0xFFDCD4EA),
    error = Color(0xFFDC2626),
)

private val UserDark = darkColorScheme(
    primary = Color(0xFFC4B5FD),
    onPrimary = Color(0xFF2E1065),
    primaryContainer = Color(0xFF5B21B6),
    onPrimaryContainer = Color(0xFFEDE4FF),
    secondary = Color(0xFFF9A8D4),
    onSecondary = Color(0xFF500724),
    secondaryContainer = Color(0xFF9D174D),
    onSecondaryContainer = Color(0xFFFFE0EF),
    tertiary = Color(0xFFFDBA74),
    onTertiary = Color(0xFF431407),
    tertiaryContainer = Color(0xFF9A3412),
    onTertiaryContainer = Color(0xFFFFE7D3),
    background = Color(0xFF120E1C),
    onBackground = Color(0xFFF1ECFA),
    surface = Color(0xFF120E1C),
    onSurface = Color(0xFFF1ECFA),
    surfaceVariant = Color(0xFF2A2338),
    onSurfaceVariant = Color(0xFFC9C0DA),
    surfaceContainerLowest = Color(0xFF0D0A15),
    surfaceContainerLow = Color(0xFF1B1528),
    surfaceContainer = Color(0xFF211A30),
    surfaceContainerHigh = Color(0xFF2A2239),
    surfaceContainerHighest = Color(0xFF332A44),
    outline = Color(0xFF8E86A3),
    outlineVariant = Color(0xFF3A3249),
    error = Color(0xFFFCA5A5),
)

private val AdminDark = darkColorScheme(
    primary = Color(0xFF22D3EE),
    onPrimary = Color(0xFF042F36),
    primaryContainer = Color(0xFF0E4A5A),
    onPrimaryContainer = Color(0xFFCFFAFE),
    secondary = Color(0xFFA3E635),
    onSecondary = Color(0xFF1A2E05),
    secondaryContainer = Color(0xFF365314),
    onSecondaryContainer = Color(0xFFECFCCB),
    tertiary = Color(0xFFF472B6),
    onTertiary = Color(0xFF500724),
    tertiaryContainer = Color(0xFF831843),
    onTertiaryContainer = Color(0xFFFCE7F3),
    background = Color(0xFF070B17),
    onBackground = Color(0xFFE6EDF7),
    surface = Color(0xFF070B17),
    onSurface = Color(0xFFE6EDF7),
    surfaceVariant = Color(0xFF1A2338),
    onSurfaceVariant = Color(0xFF9AA8C2),
    surfaceContainerLowest = Color(0xFF05080F),
    surfaceContainerLow = Color(0xFF0E1526),
    surfaceContainer = Color(0xFF121A2E),
    surfaceContainerHigh = Color(0xFF182238),
    surfaceContainerHighest = Color(0xFF1F2A44),
    outline = Color(0xFF5B6884),
    outlineVariant = Color(0xFF243049),
    error = Color(0xFFFB7185),
)

/** Status colours (always paired with an icon or text, never colour alone). */
@Immutable
data class StatusColors(
    val info: Color, val onInfo: Color,
    val ok: Color, val onOk: Color,
    val warn: Color, val onWarn: Color,
    val bad: Color, val onBad: Color,
    val mute: Color, val onMute: Color,
)

private val LightStatus = StatusColors(
    info = Color(0xFFE0E7FF), onInfo = Color(0xFF3730A3),
    ok = Color(0xFFDCFCE7), onOk = Color(0xFF166534),
    warn = Color(0xFFFEF3C7), onWarn = Color(0xFF92400E),
    bad = Color(0xFFFFE4E6), onBad = Color(0xFF9F1239),
    mute = Color(0xFFF1F0F5), onMute = Color(0xFF52525B),
)

private val DarkStatus = StatusColors(
    info = Color(0xFF1E2A5A), onInfo = Color(0xFFC7D2FE),
    ok = Color(0xFF113B24), onOk = Color(0xFFBBF7D0),
    warn = Color(0xFF4A3410), onWarn = Color(0xFFFDE68A),
    bad = Color(0xFF4C1325), onBad = Color(0xFFFECDD3),
    mute = Color(0xFF262A36), onMute = Color(0xFFD4D4D8),
)

val LocalStatusColors = staticCompositionLocalOf { LightStatus }

@Composable
private fun BaseTheme(scheme: ColorScheme, status: StatusColors, brand: Brand, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalStatusColors provides status, LocalBrand provides brand) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
    }
}

/** The user app (and sign-in screens): bright, playful, follows the phone's dark mode. */
@Composable
fun UserTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) =
    BaseTheme(if (darkTheme) UserDark else UserLight, if (darkTheme) DarkStatus else LightStatus, UserBrand, content)

/** The admin app: always the dark neon command centre, so nobody confuses the two. */
@Composable
fun AdminTheme(content: @Composable () -> Unit) = BaseTheme(AdminDark, DarkStatus, AdminBrand, content)

/** Kept for UI tests written against the earlier build. */
@Composable
fun HelpdeskTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) = UserTheme(darkTheme, content)
