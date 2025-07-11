package io.silv.common.model

import androidx.compose.runtime.Stable

@Stable
enum class CardType(val string: String) {
    SemiCompact("Semi-Compact Card"),
    Compact("Compact Card"),
    ExtraCompact("Extra-Compact Card"),
}