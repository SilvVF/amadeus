package io.silv.ui.layout

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SheetValue.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberExpandableState(startProgress: SheetValue) = rememberSaveable(
    saver = Saver(
        save = { it.progress },
        restore = { ExpandableState(it) }
    ),
) {
    ExpandableState(startProgress)
}

@OptIn(ExperimentalMaterial3Api::class)
class ExpandableState(
    startProgress: SheetValue
) {
    internal var progress by mutableStateOf<SheetValue>(startProgress)

    fun hide() {
        progress = Hidden
    }

    fun expand() {
        progress = Expanded
    }

    fun show() {
        progress = PartiallyExpanded
    }

    fun toggleProgress() {
        progress = when (progress) {
            Hidden -> PartiallyExpanded
            Expanded -> Hidden
            PartiallyExpanded -> Expanded
        }
    }
}

private sealed interface DragAction {
    data class Stopped(val velocity: Float): DragAction
    data class Start(val offset: Offset): DragAction
    data class Drag(val delta: Float): DragAction
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandableInfoLayout(
    state: ExpandableState = rememberExpandableState(startProgress = Hidden),
    peekContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    var dragHeightOffset by remember {
        mutableFloatStateOf(0f)
    }

    var maxHeightPx by remember {
        mutableIntStateOf(0)
    }
    var peekHeightPx by remember {
        mutableIntStateOf(0)
    }

    val dragChannel = remember { Channel<DragAction>() }

    val dragState = rememberDraggableState {
        dragChannel.trySend(DragAction.Drag(it))
    }

    LaunchedEffect(dragChannel) {
        withContext(Dispatchers.Main.immediate) {
            dragChannel.consumeAsFlow().collect { action ->
                when (action) {
                    is DragAction.Drag -> {
                        dragHeightOffset += action.delta
                    }
                    is DragAction.Start -> {
                        dragHeightOffset = 0f
                    }
                    is DragAction.Stopped -> {

                        val progress = state.progress
                        val initialHeight = when (progress) {
                            Expanded -> maxHeightPx
                            PartiallyExpanded -> peekHeightPx
                            else -> 0
                        }

                        val dragEndHeight = initialHeight - dragHeightOffset

                        dragHeightOffset = 0f

                        state.progress = when (progress) {
                            PartiallyExpanded -> when {
                                dragEndHeight < peekHeightPx / 3 -> Hidden
                                dragEndHeight > peekHeightPx -> Expanded
                                else -> PartiallyExpanded
                            }
                            Expanded -> when {
                                dragEndHeight < peekHeightPx / 3 -> Hidden
                                dragEndHeight < maxHeightPx - (peekHeightPx / 3) -> PartiallyExpanded
                                else -> Expanded
                            }
                            else -> progress
                        }
                    }
                }
            }
        }
    }

    Layout(
        content = {
            peekContent()
            content()
        },
        modifier = Modifier
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStarted = {
                    dragChannel.send(DragAction.Start(it))
                },
                onDragStopped = {
                    dragChannel.send(DragAction.Stopped(it))
                }
            )
            .animateContentSize()
    ) { measurable, constraints ->

        val placeables = measurable.map { it.measure(constraints) }

        val peekHeight = placeables.first().height.also { peekHeightPx = it }

        val maxHeight = placeables.sumOf { it.height }.also { maxHeightPx = it }

        val height = when(state.progress) {
            Hidden -> -10
            Expanded ->
                (maxHeight + -dragHeightOffset.roundToInt()).coerceAtMost(maxHeight)
            PartiallyExpanded ->
                (peekHeight + -dragHeightOffset.roundToInt()).coerceAtMost(maxHeight)
        }

        layout(constraints.maxWidth, height) {

            var y = 0

            placeables.fastForEach { placeable ->
                placeable.placeRelative(
                    x = 0,
                    y = y.also { y += placeable.height }
                )
            }
        }
    }
}