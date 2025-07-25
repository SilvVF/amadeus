package io.silv.ui.layout

import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.animateToWithDecay
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun rememberExpandableState(
    startProgress: DragAnchors = DragAnchors.End
): ExpandableState {
    val state = remember {
        AnchoredDraggableState(
            initialValue = startProgress
        )
    }
    val scope = rememberCoroutineScope()
    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(state)

    return rememberSaveable<ExpandableState>(
        saver =
            Saver<ExpandableState, DragAnchors>(
                save = { it.anchoredDraggableState.currentValue },
                restore = { anchor ->
                    ExpandableState(
                        state.apply {
                            scope.launch {
                                snapTo(anchor)
                            }
                        },
                        flingBehavior
                    )
                },
            ),
    ) {
        ExpandableState(
            state,
            flingBehavior
        )
    }
}

@Stable
enum class DragAnchors {
    Start,
    Peek,
    End,
}

@LayoutScopeMarker
@Immutable
class ExpandableScope(private val expandableState: ExpandableState) {

    @Stable
    fun Modifier.nestedScroll(
        scrollState: ScrollableState
    ): Modifier = composed {

        val connection = remember(scrollState) {
            expandableState.nestedScrollConnection(scrollState)
        }

        this.nestedScroll(connection)
    }
}

class ExpandableState(
    val anchoredDraggableState: AnchoredDraggableState<DragAnchors>,
    val flingBehavior: TargetedFlingBehavior
) {
    internal var maxHeightPx by mutableIntStateOf(0)

    internal fun nestedScrollConnection(
        scrollState: ScrollableState
    ) = object : NestedScrollConnection {
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
                val consumedY = anchoredDraggableState.animateToWithDecay(
                    anchoredDraggableState.targetValue,
                    available.y
                )
                Velocity(0f, consumedY)
            } else {
                Velocity.Zero
            }
        }

        override suspend fun onPostFling(
            consumed: Velocity,
            available: Velocity
        ): Velocity {
            val consumedY = anchoredDraggableState.animateToWithDecay(
                anchoredDraggableState.targetValue,
                available.y
            )
            return Velocity(0f, consumedY)
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

    suspend fun toggle() {
        when (anchoredDraggableState.targetValue) {
            DragAnchors.Start -> anchoredDraggableState.animateTo(DragAnchors.End)
            DragAnchors.Peek -> anchoredDraggableState.animateTo(DragAnchors.Start)
            DragAnchors.End -> anchoredDraggableState.animateTo(DragAnchors.Peek)
        }
    }
}

@Composable
fun ExpandableInfoLayout(
    modifier: Modifier = Modifier,
    state: ExpandableState = rememberExpandableState(startProgress = DragAnchors.End),
    peekContent: @Composable () -> Unit = {},
    content: @Composable ExpandableScope.() -> Unit = {},
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
                ExpandableScope(state).content()
            }
        },
        modifier =
            modifier
                .wrapContentHeight()
                .anchoredDraggable(
                    state.anchoredDraggableState,
                    flingBehavior = state.flingBehavior,
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

        if (
            totalHeight != state.maxHeightPx ||
            state.anchoredDraggableState.offset.isNaN()
        ) {
            state.maxHeightPx = totalHeight
            state.anchoredDraggableState.updateAnchors(
                DraggableAnchors {
                    DragAnchors.entries
                        .forEach { anchor ->
                            anchor at
                                    when (anchor) {
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