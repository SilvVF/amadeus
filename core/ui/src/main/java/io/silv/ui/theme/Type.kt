package io.silv.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.silv.ui.R

private val defaultTypography = Typography()

private val montserrat =
    FontFamily(
        Font(R.font.montserrat_thin, FontWeight.Thin),
        Font(R.font.montserrat_black, FontWeight.Black),
        Font(R.font.montserrat_bold, FontWeight.Bold),
        Font(R.font.montserrat_extrabold, FontWeight.ExtraBold),
        Font(R.font.montserrat_medium, FontWeight.Medium),
        Font(R.font.montserrat_semibold, FontWeight.SemiBold),
        Font(R.font.montserrat_regular, FontWeight.Normal),
    )

private val appTypography =
    Typography(
        displayLarge = defaultTypography.displayLarge.copy(fontFamily = montserrat),
        displayMedium = defaultTypography.displayMedium.copy(fontFamily = montserrat),
        displaySmall = defaultTypography.displaySmall.copy(fontFamily = montserrat),
        headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = montserrat),
        headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = montserrat),
        headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = montserrat),
        titleLarge = defaultTypography.titleLarge.copy(fontFamily = montserrat),
        titleMedium = defaultTypography.titleMedium.copy(fontFamily = montserrat),
        titleSmall = defaultTypography.titleSmall.copy(fontFamily = montserrat),
        bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = montserrat),
        bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = montserrat),
        bodySmall = defaultTypography.bodySmall.copy(fontFamily = montserrat),
        labelLarge = defaultTypography.labelLarge.copy(fontFamily = montserrat),
        labelMedium = defaultTypography.labelMedium.copy(fontFamily = montserrat),
        labelSmall = defaultTypography.labelSmall.copy(fontFamily = montserrat),
    )

val Typography = appTypography
