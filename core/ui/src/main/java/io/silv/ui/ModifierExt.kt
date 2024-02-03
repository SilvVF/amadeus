package io.silv.ui

import androidx.annotation.FloatRange
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt


fun Modifier.vertical() =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.height, placeable.width) {
            placeable.place(
                x = -(placeable.width / 2 - placeable.height / 2),
                y = -(placeable.height / 2 - placeable.width / 2),
            )
        }
    }


context(BoxScope)
fun Modifier.fillMaxSizeAfterMeasure(
    @FloatRange(0.0, 1.0, true, true) size: Float,
) = this
        .matchParentSize()
        .layout { measurable, constraints ->
            // Measure the composable
            val placeable = measurable.measure(constraints)

            layout(placeable.width, placeable.height) {
                placeable.place(0, -(constraints.maxHeight * (1f - size)).roundToInt())
            }
        }


fun Modifier.conditional(
    condition: Boolean,
    modifier: Modifier.() -> Modifier,
): Modifier {
    return if (condition) {
        then(modifier(Modifier))
    } else {
        this
    }
}


fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier =
    this.composed {
        clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
        ) {
            onClick()
        }
    }
