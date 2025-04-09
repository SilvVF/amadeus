package io.silv.amadeus

import androidx.activity.compose.PredictiveBackHandler
import androidx.collection.mutableObjectFloatMapOf
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.launch
import kotlin.collections.plus
import kotlin.coroutines.cancellation.CancellationException

//https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:navigation/navigation-compose/src/commonMain/kotlin/androidx/navigation/compose/NavHost.kt;l=559;drc=78fdd43c1462ed48b757ed3f9234f6f949fd4004;bpv=1;bpt=1
@Composable
fun PredictiveBackScaleTransition(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedContentScope.(Screen) -> Unit = { it.Content() },
) {
    PredictiveBackScreenTransition(
        navigator = navigator,
        modifier = modifier,
        popExitTransition = {
            scaleOut(
                targetScale = 0.9f,
                transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0.5f)
            )
        },
        popEnterTransition = {
            EnterTransition.None
        },
        content = content,
    )
}

@OptIn(ExperimentalAnimationApi::class, InternalVoyagerApi::class, ExperimentalVoyagerApi::class)
@Composable
public fun PredictiveBackScreenTransition(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    disposeScreenAfterTransitionEnd: Boolean = false,
    contentKey: (Screen) -> Any = { it.key },
    enterTransition:
    (@JvmSuppressWildcards
    AnimatedContentTransitionScope<Screen>.() -> EnterTransition) =
        {
            fadeIn(animationSpec = tween(700))
        },
    exitTransition:
    (@JvmSuppressWildcards
    AnimatedContentTransitionScope<Screen>.() -> ExitTransition) =
        {
            fadeOut(animationSpec = tween(700))
        },
    popEnterTransition:
    (@JvmSuppressWildcards
    AnimatedContentTransitionScope<Screen>.() -> EnterTransition) =
        enterTransition,
    popExitTransition:
    (@JvmSuppressWildcards
    AnimatedContentTransitionScope<Screen>.() -> ExitTransition) =
        exitTransition,
    sizeTransform:
    (@JvmSuppressWildcards
    AnimatedContentTransitionScope<Screen>.() -> SizeTransform?)? =
        null,
    content: @Composable AnimatedContentScope.(Screen) -> Unit
) {
    val screenCandidatesToDispose = rememberSaveable(saver = screenCandidatesToDisposeSaver()) {
        mutableStateOf(emptySet())
    }

    val currentScreens = navigator.items

    if (disposeScreenAfterTransitionEnd) {
        DisposableEffect(currentScreens) {
            onDispose {
                val newScreenKeys = navigator.items.map { it.key }
                screenCandidatesToDispose.value += currentScreens.filter { it.key !in newScreenKeys }
            }
        }
    }

    var progress by remember { mutableFloatStateOf(0f) }
    var inPredictiveBack by remember { mutableStateOf(false) }

    PredictiveBackHandler(navigator.items.size > 1) { backEvent ->
        try {
            backEvent.collect { event ->
                inPredictiveBack = true
                progress = event.progress
            }
            // code for completion
            if (navigator.items.size > 1) {
                inPredictiveBack = false
                navigator.pop()
            }
        } catch (e: CancellationException) {
            if (navigator.items.size > 1) {
                inPredictiveBack = false
            }
        }
    }

    val backStackEntry: Screen? = navigator.lastItemOrNull
    val zIndices = remember { mutableObjectFloatMapOf<String>() }

    if (backStackEntry != null) {

        val finalEnter: AnimatedContentTransitionScope<Screen>.() -> EnterTransition = {
            if (navigator.lastEvent == StackEvent.Pop || inPredictiveBack) {
                popEnterTransition.invoke(this)
            } else {
                enterTransition.invoke(this)
            }
        }

        val finalExit: AnimatedContentTransitionScope<Screen>.() -> ExitTransition = {
            if (navigator.lastEvent == StackEvent.Pop || inPredictiveBack) {
                popExitTransition.invoke(this)
            } else {
                exitTransition.invoke(this)
            }
        }

        val finalSizeTransform: AnimatedContentTransitionScope<Screen>.() -> SizeTransform? = {
            sizeTransform?.invoke(this)
        }

        val transitionState = remember {
            // The state returned here cannot be nullable cause it produces the input of the
            // transitionSpec passed into the AnimatedContent and that must match the non-nullable
            // scope exposed by the transitions on the NavHost and composable APIs.
            SeekableTransitionState(backStackEntry)
        }
        val transition = rememberTransition(transitionState, label = "entry")

        if (inPredictiveBack) {
            LaunchedEffect(progress) {
                val previousEntry = navigator.items[navigator.items.size - 2]
                transitionState.seekTo(progress, previousEntry)
            }
        } else {
            LaunchedEffect(navigator.lastItem) {
                // This ensures we don't animate after the back gesture is cancelled and we
                // are already on the current state
                if (transitionState.currentState != backStackEntry) {
                    transitionState.animateTo(backStackEntry)
                } else {
                    // convert from nanoseconds to milliseconds
                    val totalDuration = transition.totalDurationNanos / 1000000
                    // When the predictive back gesture is cancel, we need to manually animate
                    // the SeekableTransitionState from where it left off, to zero and then
                    // snapTo the final position.
                    animate(
                        transitionState.fraction,
                        0f,
                        animationSpec = tween((transitionState.fraction * totalDuration).toInt())
                    ) { value, _ ->
                        this@LaunchedEffect.launch {
                            if (value > 0) {
                                // Seek the original transition back to the currentState
                                transitionState.seekTo(value)
                            }
                            if (value == 0f) {
                                // Once we animate to the start, we need to snap to the right state.
                                transitionState.snapTo(backStackEntry)
                            }
                        }
                    }
                }
            }
        }

        transition.AnimatedContent(
            modifier,
            transitionSpec = {
                // If the initialState of the AnimatedContent is not in visibleEntries, we are in
                // a case where visible has cleared the old state for some reason, so instead of
                // attempting to animate away from the initialState, we skip the animation.
                if (initialState in navigator.items) {
                    val initialZIndex = zIndices.getOrPut(initialState.key) { 0f }
                    val targetZIndex =
                        when {
                            targetState == initialState -> initialZIndex
                            navigator.lastEvent == StackEvent.Pop || inPredictiveBack -> initialZIndex - 1f
                            else -> initialZIndex + 1f
                        }
                    zIndices[targetState.key] = targetZIndex

                    ContentTransform(
                        finalEnter(this),
                        finalExit(this),
                        targetZIndex,
                        finalSizeTransform(this)
                    )
                } else {
                    EnterTransition.None togetherWith ExitTransition.None
                }
            },
            contentAlignment,
            contentKey
        ) {

            val isPredictiveBackCancelAnimation = transitionState.currentState == navigator.lastItem
            val currentEntry =
                if (inPredictiveBack || isPredictiveBackCancelAnimation) {
                    // We have to do this because the previous entry does not show up in
                    // visibleEntries
                    // even if we prepare it above as part of onBackStackChangeStarted
                    it
                } else {
                    currentScreens.lastOrNull { entry -> it == entry }
                }

            if (this.transition.targetState == this.transition.currentState && disposeScreenAfterTransitionEnd) {
                LaunchedEffect(Unit) {
                    val newScreens = navigator.items.map { it.key }
                    val screensToDispose =
                        screenCandidatesToDispose.value.filterNot { it.key in newScreens }
                    if (screensToDispose.isNotEmpty()) {
                        screensToDispose.forEach { navigator.dispose(it) }
                        navigator.clearEvent()
                    }
                    screenCandidatesToDispose.value = emptySet()
                }
            }

            currentEntry?.let {
                navigator.saveableState("transition", it) {
                    content(it)
                }
            }
        }
        LaunchedEffect(transition.currentState, transition.targetState) {
            if (
                transition.currentState == transition.targetState &&
                // There is a race condition where previous animation has completed the new
                // animation has yet to start and there is a navigate call before this effect.
                // We need to make sure we are completing only when the start is settled on the
                // actual entry.
                (navigator.lastItemOrNull == null ||
                        transition.targetState == navigator.lastItem)
            ) {
                zIndices.removeIf { key, _ -> key != transition.targetState.key }
            }
        }
    }
}

private fun screenCandidatesToDisposeSaver(): Saver<MutableState<Set<Screen>>, List<Screen>> {
    return Saver(
        save = { it.value.toList() },
        restore = { mutableStateOf(it.toSet()) }
    )
}