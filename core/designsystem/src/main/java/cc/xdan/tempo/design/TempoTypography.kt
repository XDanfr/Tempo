package cc.xdan.tempo.design

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import cc.xdan.tempo.core.designsystem.R

private val outfit = FontFamily(
    Font(R.font.outfit, FontWeight.Normal),
    Font(R.font.outfit, FontWeight.Medium),
    Font(R.font.outfit, FontWeight.SemiBold),
    Font(R.font.outfit, FontWeight.Bold),
)
private val base = Typography()
val TempoTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = outfit),
    displayMedium = base.displayMedium.copy(fontFamily = outfit),
    displaySmall = base.displaySmall.copy(fontFamily = outfit),
    headlineLarge = base.headlineLarge.copy(fontFamily = outfit),
    headlineMedium = base.headlineMedium.copy(fontFamily = outfit),
    headlineSmall = base.headlineSmall.copy(fontFamily = outfit),
    titleLarge = base.titleLarge.copy(fontFamily = outfit),
    titleMedium = base.titleMedium.copy(fontFamily = outfit),
    titleSmall = base.titleSmall.copy(fontFamily = outfit),
    bodyLarge = base.bodyLarge.copy(fontFamily = outfit),
    bodyMedium = base.bodyMedium.copy(fontFamily = outfit),
    bodySmall = base.bodySmall.copy(fontFamily = outfit),
    labelLarge = base.labelLarge.copy(fontFamily = outfit),
    labelMedium = base.labelMedium.copy(fontFamily = outfit),
    labelSmall = base.labelSmall.copy(fontFamily = outfit),
)
