@file:OptIn(ExperimentalFoundationApi::class)

package io.silv.ui.layout

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberExpandableState(
    startProgress: DragAnchors = DragAnchors.End,
    density: Density = LocalDensity.current
) = rememberSaveable(
    saver =
    Saver(
        save = { it.anchoredDraggableState.currentValue },
        restore = { ExpandableState(it, density) },
    ),
) {
    ExpandableState(
        startProgress,
        density,
    )
}

enum class DragAnchors {
    Start,
    Peek,
    End,
}

class ExpandableState(
    startProgress: DragAnchors,
    density: Density,
) {

    internal var maxHeightPx by mutableIntStateOf(0)

    @OptIn(ExperimentalFoundationApi::class)
    internal val anchoredDraggableState = AnchoredDraggableState(
        initialValue = startProgress,
        positionalThreshold = { distance: Float -> distance * 0.5f },
        velocityThreshold = { with(density) { 100.dp.toPx() } },
        animationSpec = tween(),
    )

    fun nestedScrollConnection(
        scrollState: ScrollableState
    ) = object: NestedScrollConnection {
        override fun onPreScroll(
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            val delta = available.y
            return if (delta < 0) {
                Offset(
                    x = available.x,
                    y = anchoredDraggableState.dispatchRawDelta(delta)
                )
            } else {
                Offset.Zero
            }
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            val delta = available.y
            return Offset(
                x = available.x,
                y = anchoredDraggableState.dispatchRawDelta(delta)
            )
        }
        override suspend fun onPreFling(available: Velocity): Velocity {
            return if (available.y < 0 && !scrollState.canScrollBackward) {
                anchoredDraggableState.settle(available.y)
                available
            } else {
                Velocity.Zero
            }
        }

        override suspend fun onPostFling(
            consumed: Velocity,
            available: Velocity
        ): Velocity {
            anchoredDraggableState.settle(available.y)
            return super.onPostFling(consumed, available)
        }
    }

    val fraction by lazy {
        derivedStateOf {
            (anchoredDraggableState.offset / maxHeightPx)
                .takeIf { !it.isNaN() }
                ?.coerceIn(0f, 1f)
                ?: 0f
        }
    }

    val isHidden by derivedStateOf {
        anchoredDraggableState.currentValue == DragAnchors.End
    }

    suspend fun hide() {
        anchoredDraggableState.animateTo(DragAnchors.End)
    }

    suspend fun expand() {
        anchoredDraggableState.animateTo(DragAnchors.Start)
    }

    suspend fun show() {
        anchoredDraggableState.animateTo(DragAnchors.Peek)
    }

    suspend fun toggleProgress() {
        when (anchoredDraggableState.targetValue) {
            DragAnchors.Start -> anchoredDraggableState.animateTo(DragAnchors.End)
            DragAnchors.Peek -> anchoredDraggableState.animateTo(DragAnchors.Start)
            DragAnchors.End -> anchoredDraggableState.animateTo(DragAnchors.Peek)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExpandableInfoLayout(
    modifier: Modifier = Modifier,
    state: ExpandableState = rememberExpandableState(startProgress = DragAnchors.End),
    peekContent: @Composable () -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    Layout(
        {
            Box(
                Modifier
                    .layoutId("peekContent")
            ) {
                peekContent()
            }
            Box(
                Modifier
                    .layoutId("content"),
            ) {
                content()
            }
        },
        modifier =
        modifier
            .wrapContentHeight()
            .anchoredDraggable(
                state.anchoredDraggableState,
                orientation = Orientation.Vertical
            ),
    ) { measurables, constraints ->

        val peekPlaceable =
            measurables.first { it.layoutId == "peekContent" }
                .measure(constraints.copy(minWidth = 0))

        val contentPlaceable =
            measurables.first { it.layoutId == "content" }
                .measure(constraints.copy(minWidth = 0))

        val totalHeight = peekPlaceable.height + contentPlaceable.height

        if (totalHeight != state.maxHeightPx || state.anchoredDraggableState.offset.isNaN()) {
            state.maxHeightPx = totalHeight
            state.anchoredDraggableState.updateAnchors(
                DraggableAnchors {
                    DragAnchors.values()
                        .forEach { anchor ->
                            anchor at
                                    when(anchor) {
                                        DragAnchors.Start -> 0f
                                        DragAnchors.Peek -> contentPlaceable.height.toFloat()
                                        DragAnchors.End -> totalHeight.toFloat()
                                    }
                        }
                }
            )
        }

        val height = totalHeight - state.anchoredDraggableState.requireOffset()

        layout(
            width = constraints.maxWidth,
            height = height.roundToInt()
        ) {

            var y = 0

            peekPlaceable.placeRelative(
                x = 0,
                y = 0.also { y += peekPlaceable.height },
            )

            contentPlaceable.placeRelative(
                x = 0,
                y = y,
            )
        }
    }
}
