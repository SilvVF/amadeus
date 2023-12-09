package io.silv.ui.layout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SheetValue.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberExpandableState(
    startProgress: SheetValue,
    scope: CoroutineScope = rememberCoroutineScope(),
    decay: DecayAnimationSpec<Float> =  rememberSplineBasedDecay<Float>()
) = rememberSaveable(
    saver = Saver(
        save = { it.progress },
        restore = { ExpandableState(it, scope, decay) }
    ),
) {
    ExpandableState(
        startProgress,
        scope,
        decay
    )
}

internal sealed interface DragAction {
    data class Drag(val delta: Float): DragAction
    data class Stopped(val velocity: Float): DragAction
    data class Start(val offset: Offset): DragAction
}

@OptIn(ExperimentalMaterial3Api::class)
class ExpandableState(
    startProgress: SheetValue,
    private val scope: CoroutineScope,
    decay: DecayAnimationSpec<Float>,
) {
    internal val dragChannel: Channel<DragAction> = Channel(Channel.UNLIMITED)

    internal val dragState: DraggableState = DraggableState { delta ->
        dragChannel.trySend(DragAction.Drag(delta))
    }

    var progress by mutableStateOf(startProgress)

    val isExpanded by derivedStateOf {
        progress == Expanded
    }

    val isHidden by derivedStateOf {
        progress == Hidden
    }

    internal var maxHeightPx by mutableIntStateOf(0)

    internal var peekHeightPx by mutableIntStateOf(0)

    // Derived state allows animatable initial value to be set by layout
    // before it is used to calculate height based on max and peek heights
    internal val dragHeightOffset by derivedStateOf {
        Animatable(
            when (progress) {
                Hidden -> maxHeightPx.toFloat()
                Expanded -> 0f
                PartiallyExpanded -> maxHeightPx.toFloat() - peekHeightPx.toFloat()
            }
        )
    }

    init {
        scope.launch {
            withContext(Dispatchers.Main.immediate) {
                dragChannel.consumeAsFlow().collectLatest { action ->
                    when (action) {
                        is DragAction.Drag -> {
                            dragHeightOffset.snapTo(dragHeightOffset.value + action.delta)
                        }
                        is DragAction.Start -> {
                            val targetY = when(progress) {
                                Hidden -> maxHeightPx.toFloat()
                                Expanded -> 0f
                                PartiallyExpanded -> maxHeightPx.toFloat() - peekHeightPx.toFloat()
                            }
                            dragHeightOffset.animateTo(targetY, initialVelocity = dragHeightOffset.velocity)
                        }
                        is DragAction.Stopped -> {

                            val useDecay = !(dragHeightOffset.value >= maxHeightPx * 0.99f || dragHeightOffset.value <= 0.01f)

                            val height = maxHeightPx - if (useDecay)
                                decay.calculateTargetValue(dragHeightOffset.value, action.velocity)
                            else
                                dragHeightOffset.value


                            val newProgress = when  {
                                height > maxHeightPx - peekHeightPx / 2 -> Expanded
                                height > (maxHeightPx - peekHeightPx) / 2 -> PartiallyExpanded
                                else -> Hidden
                            }

                            val canReachNewState = newProgress != progress

                            val targetY = when(newProgress) {
                                Hidden -> maxHeightPx.toFloat()
                                Expanded -> 0f
                                PartiallyExpanded -> maxHeightPx.toFloat() -  peekHeightPx.toFloat()
                            }

                            val result = if (canReachNewState && useDecay) {
                                dragHeightOffset.animateDecay(action.velocity, decay).endState.velocity
                            } else {
                                dragHeightOffset.velocity
                            }

                            dragHeightOffset.animateTo(targetY, initialVelocity = result)

                            progress = newProgress
                        }
                    }
                }
            }
        }
    }

    private val animSpec: TweenSpec<Float> = tween(300,0, LinearOutSlowInEasing)

    private var jobToTarget: Pair<Job, SheetValue>? = null
        set(value) {
            scope.launch {
                value?.first?.invokeOnCompletion {
                    if (it == null) // job completed normally
                        field = null
                }
            }
            field = value
        }


    suspend fun hide() {

        jobToTarget.let { it?.first?.cancel() }

        val job = scope.launch {
            dragHeightOffset.animateTo(
                maxHeightPx.toFloat(),
                animSpec,
                initialVelocity = dragHeightOffset.velocity
            )
            progress = Hidden
        }

        jobToTarget = job to Hidden
    }

    suspend fun expand() {

        jobToTarget.let { it?.first?.cancel() }

        val job = scope.launch {
            dragHeightOffset.animateTo(
                0f,
                animSpec,
                initialVelocity = dragHeightOffset.velocity
            )
            progress = Expanded
        }

        jobToTarget = job to Expanded
    }

    suspend fun show() {

        jobToTarget.let { it?.first?.cancel() }

        val job = scope.launch {
            dragHeightOffset.animateTo(
                maxHeightPx.toFloat() - peekHeightPx.toFloat(),
                animSpec,
                initialVelocity = dragHeightOffset.velocity
            )
            progress = PartiallyExpanded
        }

        jobToTarget = job to PartiallyExpanded
    }

    suspend fun toggleProgress() {
        when (jobToTarget?.second ?: progress) {
            Hidden -> show()
            Expanded -> hide()
            PartiallyExpanded -> expand()
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandableInfoLayout(
    modifier: Modifier = Modifier,
    state: ExpandableState = rememberExpandableState(startProgress = Hidden),
    peekContent: @Composable () -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    Layout(
        {
            Box(
                Modifier.layoutId("peekContent")
                    .windowInsetsPadding(WindowInsets.systemBars)
            ) {
                peekContent()
            }
            Box(
                Modifier.layoutId("content")
            ) {
                content()
            }
        },
        modifier = modifier
            .draggable(
                state = state.dragState,
                orientation = Orientation.Vertical,
                onDragStarted = {
                    state.dragChannel.trySend(DragAction.Start(it))
                },
                onDragStopped = {
                    state.dragChannel.trySend(DragAction.Stopped(it))
                }
            )
    ) { measurables, constraints ->

        val peekPlaceable =  measurables.first { it.layoutId == "peekContent" }
            .measure(constraints.copy(minWidth = 0))

        val contentPlaceable = measurables.first{ it.layoutId == "content" }
            .measure(constraints.copy(minWidth = 0))


        state.peekHeightPx = peekPlaceable.height

        val maxHeight = peekPlaceable.height + contentPlaceable.height


        state.maxHeightPx = maxHeight

        val height = maxHeight - state.dragHeightOffset.value.roundToInt()

        layout(constraints.maxWidth, height.coerceAtMost(maxHeight)) {

            var y = 0

            peekPlaceable.placeRelative(
                x = 0,
                y = 0.also { y += peekPlaceable.height }
            )

            contentPlaceable.placeRelative(
                x = 0,
                y = y
            )
        }
    }
}