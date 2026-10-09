package cc.xdan.tempo.design

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import cc.xdan.tempo.core.designsystem.R

private val outfit = FontFamily(
    Font(R.font.outfit, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
)
private val base = Typography()
val TempoTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = outfit, fontWeight = FontWeight.Bold),
    displayMedium = base.displayMedium.copy(fontFamily = outfit, fontWeight = FontWeight.Bold),
    displaySmall = base.displaySmall.copy(fontFamily = outfit, fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontFamily = outfit, fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontFamily = outfit, fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontFamily = outfit, fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontFamily = outfit, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontFamily = outfit, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontFamily = outfit, fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(fontFamily = outfit, fontWeight = FontWeight.Medium),
    bodyMedium = base.bodyMedium.copy(fontFamily = outfit, fontWeight = FontWeight.Medium),
    bodySmall = base.bodySmall.copy(fontFamily = outfit, fontWeight = FontWeight.Medium),
    labelLarge = base.labelLarge.copy(fontFamily = outfit, fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.copy(fontFamily = outfit, fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall.copy(fontFamily = outfit, fontWeight = FontWeight.SemiBold),
)
