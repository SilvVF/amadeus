@file:OptIn(ExperimentalSharedTransitionApi::class)

package io.silv.ui

import androidx.annotation.FloatRange
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize.Companion.contentSize
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.animation.core.Spring.StiffnessMediumLow
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
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


fun Modifier.fillMaxSizeAfterMeasure(
    scope: BoxScope,
    @FloatRange(0.0, 1.0, true, true) size: Float,
): Modifier {
    return with(scope) {
        this@fillMaxSizeAfterMeasure
            .matchParentSize()
            .layout { measurable, constraints ->
                // Measure the composable
                val placeable = measurable.measure(constraints)

                layout(placeable.width, placeable.height) {
                    placeable.place(0, -(constraints.maxHeight * (1f - size)).roundToInt())
                }
            }
    }
}

fun Modifier.tryApplySharedElementTransition(
    key: () -> Any,
    boundsTransform: BoundsTransform = BoundsTransform { _, _ -> spring(
        stiffness = StiffnessMediumLow,
        visibilityThreshold = Rect.VisibilityThreshold
    ) },
    placeHolderSize: PlaceHolderSize = contentSize,
    renderInOverlayDuringTransition: Boolean = true,
    zIndexInOverlay: Float = 0f,
    clipInOverlayDuringTransition: OverlayClip = object : OverlayClip {
        override fun getClipPath(
            state: SharedContentState,
            bounds: Rect,
            layoutDirection: LayoutDirection,
            density: Density
        ): Path? {
            return state.parentSharedContentState?.clipPathInOverlay
        }
    }
): Modifier = composed {
    val scope = LocalTransitionScope.current
    val animationScope = LocalAnimatedContentScope.current

    if (scope != null && animationScope != null) {
        with(scope) {
            this@composed.then(
                Modifier.sharedElement(
                    scope.rememberSharedContentState(key()),
                    animationScope,
                    boundsTransform,
                    placeHolderSize,
                    renderInOverlayDuringTransition,
                    zIndexInOverlay,
                    clipInOverlayDuringTransition
                )
            )
        }
    } else {
        this
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
