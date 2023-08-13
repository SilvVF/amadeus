package io.silv.amadeus.data

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.serialization.Serializable

@Serializable
data class ReaderSettings(
    val orientation: Orientation = Orientation.Horizontal,
    val direction: LayoutDirection = LayoutDirection.Ltr,
)
