package io.silv.ui.layout

/*
 * MIT License
 *
 * Copyright (c) 2022 Albert Chang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/**
 * Code taken from https://gist.github.com/mxalbert1996/33a360fcab2105a31e5355af98216f5a
 * with some modifications to handle contentPadding.
 *
 * Modifiers for regular scrollable list is omitted.
 */

import android.view.ViewConfiguration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastSumBy
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.sample

const val STICKY_HEADER_KEY_PREFIX = "sticky:"

/**
 * LazyColumn with scrollbar.
 */
@Composable
fun ScrollbarLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit,
) {
    val direction = LocalLayoutDirection.current
    val density = LocalDensity.current

    val positionOffset = remember(contentPadding) {
        with(density) { contentPadding.calculateEndPadding(direction).toPx() }
    }

    LazyColumn(
        modifier = modifier
            .drawVerticalScrollbar(
                state = state,
                reverseScrolling = reverseLayout,
                positionOffsetPx = positionOffset,
            ),
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        userScrollEnabled = userScrollEnabled,
        content = content,
    )
}

@Composable
fun ScrollbarLazyGrid(
    columns: GridCells,
    cellCount: Int,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyGridScope.() -> Unit
) {
    val direction = LocalLayoutDirection.current
    val density = LocalDensity.current

    val positionOffset = remember(contentPadding) {
        with(density) { contentPadding.calculateEndPadding(direction).toPx() }
    }

    LazyVerticalGrid(
        columns = columns,
        modifier = modifier
            .drawVerticalScrollbar(
                state = state,
                cellCount = cellCount,
                reverseScrolling = reverseLayout,
                positionOffsetPx = positionOffset,
            ),
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalArrangement = horizontalArrangement,
        userScrollEnabled = userScrollEnabled,
        flingBehavior = flingBehavior,
        content = content,
    )
}

/**
 * Draws horizontal scrollbar to a LazyList.
 *
 * Set key with [STICKY_HEADER_KEY_PREFIX] prefix to any sticky header item in the list.
 */
fun Modifier.drawHorizontalScrollbar(
    state: LazyListState,
    reverseScrolling: Boolean = false,
    // The amount of offset the scrollbar position towards the top of the layout
    positionOffsetPx: Float = 0f,
): Modifier = drawScrollbar(state, Orientation.Horizontal, reverseScrolling, positionOffsetPx)

/**
 * Draws vertical scrollbar to a LazyList.
 *
 * Set key with [STICKY_HEADER_KEY_PREFIX] prefix to any sticky header item in the list.
 */
fun Modifier.drawVerticalScrollbar(
    state: LazyListState,
    reverseScrolling: Boolean = false,
    // The amount of offset the scrollbar position towards the start of the layout
    positionOffsetPx: Float = 0f,
): Modifier = drawScrollbar(state, Orientation.Vertical, reverseScrolling, positionOffsetPx)

fun Modifier.drawVerticalScrollbar(
    state: LazyGridState,
    reverseScrolling: Boolean = false,
    cellCount: Int,
    // The amount of offset the scrollbar position towards the start of the layout
    positionOffsetPx: Float = 0f,
): Modifier = drawScrollbar(state, cellCount = cellCount, Orientation.Vertical, reverseScrolling, positionOffsetPx)

private fun Modifier.drawScrollbar(
    state: LazyGridState,
    cellCount: Int,
    orientation: Orientation,
    reverseScrolling: Boolean,
    positionOffset: Float,
): Modifier = drawScrollbar(
    orientation,
    reverseScrolling,
) { reverseDirection, atEnd, thickness, color, alpha ->
    val layoutInfo = state.layoutInfo
    val viewportSize = if (orientation == Orientation.Horizontal) {
        layoutInfo.viewportSize.width
    } else {
        layoutInfo.viewportSize.height
    } - layoutInfo.beforeContentPadding - layoutInfo.afterContentPadding
    val items = layoutInfo.visibleItemsInfo
    val itemsSize = items.fastSumBy {
        if (orientation == Orientation.Horizontal)
            it.size.width / cellCount
        else
            it.size.height / cellCount
    }
    val showScrollbar = items.size < layoutInfo.totalItemsCount || itemsSize > viewportSize
    val estimatedItemSize = if (items.isEmpty()) 0f else itemsSize.toFloat() / items.size
    val totalSize = estimatedItemSize * layoutInfo.totalItemsCount
    val thumbSize = viewportSize / totalSize * viewportSize
    val startOffset = if (items.isEmpty()) {
        0f
    } else {
        items
            .fastFirstOrNull { (it.key as? String)?.startsWith(STICKY_HEADER_KEY_PREFIX)?.not() ?: true }!!
            .run {
                val startPadding = if (reverseDirection) layoutInfo.afterContentPadding else layoutInfo.beforeContentPadding
                startPadding + ((estimatedItemSize * index - offset.y) / totalSize * viewportSize)
            }
    }
    val drawScrollbar = onDrawScrollbar(
        orientation, reverseDirection, atEnd, showScrollbar,
        thickness, color, alpha, thumbSize, startOffset, positionOffset,
    )
    drawContent()
    drawScrollbar()
}

private fun Modifier.drawScrollbar(
    state: LazyListState,
    orientation: Orientation,
    reverseScrolling: Boolean,
    positionOffset: Float,
): Modifier = drawScrollbar(
    orientation,
    reverseScrolling,
) { reverseDirection, atEnd, thickness, color, alpha ->
    val layoutInfo = state.layoutInfo
    val viewportSize = if (orientation == Orientation.Horizontal) {
        layoutInfo.viewportSize.width
    } else {
        layoutInfo.viewportSize.height
    } - layoutInfo.beforeContentPadding - layoutInfo.afterContentPadding
    val items = layoutInfo.visibleItemsInfo
    val itemsSize = items.fastSumBy { it.size }
    val showScrollbar = items.size < layoutInfo.totalItemsCount || itemsSize > viewportSize
    val estimatedItemSize = if (items.isEmpty()) 0f else itemsSize.toFloat() / items.size
    val totalSize = estimatedItemSize * layoutInfo.totalItemsCount
    val thumbSize = viewportSize / totalSize * viewportSize
    val startOffset = if (items.isEmpty()) {
        0f
    } else {
        items
            .fastFirstOrNull { (it.key as? String)?.startsWith(STICKY_HEADER_KEY_PREFIX)?.not() ?: true }!!
            .run {
                val startPadding = if (reverseDirection) layoutInfo.afterContentPadding else layoutInfo.beforeContentPadding
                startPadding + ((estimatedItemSize * index - offset) / totalSize * viewportSize)
            }
    }
    val drawScrollbar = onDrawScrollbar(
        orientation, reverseDirection, atEnd, showScrollbar,
        thickness, color, alpha, thumbSize, startOffset, positionOffset,
    )
    drawContent()
    drawScrollbar()
}

private fun ContentDrawScope.onDrawScrollbar(
    orientation: Orientation,
    reverseDirection: Boolean,
    atEnd: Boolean,
    showScrollbar: Boolean,
    thickness: Float,
    color: Color,
    alpha: () -> Float,
    thumbSize: Float,
    scrollOffset: Float,
    positionOffset: Float,
): DrawScope.() -> Unit {
    val topLeft = if (orientation == Orientation.Horizontal) {
        Offset(
            if (reverseDirection) size.width - scrollOffset - thumbSize else scrollOffset,
            if (atEnd) size.height - positionOffset - thickness else positionOffset,
        )
    } else {
        Offset(
            if (atEnd) size.width - positionOffset - thickness else positionOffset,
            if (reverseDirection) size.height - scrollOffset - thumbSize else scrollOffset,
        )
    }
    val size = if (orientation == Orientation.Horizontal) {
        Size(thumbSize, thickness)
    } else {
        Size(thickness, thumbSize)
    }

    return {
        if (showScrollbar) {
            drawRect(
                color = color,
                topLeft = topLeft,
                size = size,
                alpha = alpha(),
            )
        }
    }
}

@OptIn(FlowPreview::class)
private fun Modifier.drawScrollbar(
    orientation: Orientation,
    reverseScrolling: Boolean,
    onDraw: ContentDrawScope.(
        reverseDirection: Boolean,
        atEnd: Boolean,
        thickness: Float,
        color: Color,
        alpha: () -> Float,
    ) -> Unit,
): Modifier = composed {
    val scrolled = remember {
        MutableSharedFlow<Unit>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    }
    val nestedScrollConnection = remember(orientation, scrolled) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val delta = if (orientation == Orientation.Horizontal) consumed.x else consumed.y
                if (delta != 0f) scrolled.tryEmit(Unit)
                return Offset.Zero
            }
        }
    }

    val alpha = remember { Animatable(0f) }
    LaunchedEffect(scrolled, alpha) {
        scrolled
            .sample(100)
            .collectLatest {
                alpha.snapTo(1f)
                alpha.animateTo(0f, animationSpec = FadeOutAnimationSpec)
            }
    }

    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val reverseDirection = if (orientation == Orientation.Horizontal) {
        if (isLtr) reverseScrolling else !reverseScrolling
    } else {
        reverseScrolling
    }
    val atEnd = if (orientation == Orientation.Vertical) isLtr else true

    val context = LocalContext.current
    val thickness = remember { ViewConfiguration.get(context).scaledScrollBarSize.toFloat() }
    val color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    Modifier
        .nestedScroll(nestedScrollConnection)
        .drawWithContent {
            onDraw(reverseDirection, atEnd, thickness, color, alpha::value)
        }
}

private val FadeOutAnimationSpec = tween<Float>(
    durationMillis = ViewConfiguration.getScrollBarFadeDuration(),
    delayMillis = ViewConfiguration.getScrollDefaultDelay(),
)
