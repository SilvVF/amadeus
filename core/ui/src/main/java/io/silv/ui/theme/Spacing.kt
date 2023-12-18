package io.silv.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Spacing(
    val xs: Dp = 2.dp,
    val small: Dp = 4.dp,
    val med: Dp = 8.dp,
    val large: Dp = 16.dp,
    val xlarge: Dp = 32.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
