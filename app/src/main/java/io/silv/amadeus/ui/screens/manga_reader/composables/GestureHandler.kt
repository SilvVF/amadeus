package io.silv.amadeus.ui.screens.manga_reader.composables

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberGestureHandler(
    imageListSize: Int,
    verticalReaderState: LazyListState,
    horizontalReaderState: PagerState,
    scope: CoroutineScope = rememberCoroutineScope()
) = remember {
    GestureHandler(
        scope = scope,
        imageListSize = imageListSize,
        verticalReaderState = verticalReaderState,
        horizontalReaderState = horizontalReaderState
    )
}

class GestureHandler @OptIn(ExperimentalFoundationApi::class) constructor(
    private val scope: CoroutineScope,
    private val imageListSize: Int,
    private val horizontalReaderState: PagerState,
    private val verticalReaderState: LazyListState,
) {
    val firstVisibleInVertical by derivedStateOf { verticalReaderState.firstVisibleItemIndex }


    @OptIn(ExperimentalFoundationApi::class)
    fun handleBackGesture(orientation: Orientation) {
        scope.launch {
            when (orientation){
                Orientation.Vertical -> {
                    (firstVisibleInVertical - 1)
                        .takeIf { it >= 0 }
                        ?.let { verticalReaderState.animateScrollToItem(it) }
                }
                Orientation.Horizontal -> {
                    horizontalReaderState.animateScrollToPage(
                        horizontalReaderState.currentPage - 1
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    fun handleForwardGesture(orientation: Orientation) {
        scope.launch {
            when (orientation){
                Orientation.Vertical -> {
                    (firstVisibleInVertical + verticalReaderState.layoutInfo.visibleItemsInfo.size)
                        .takeIf { it in 0 until imageListSize }
                        ?.let { verticalReaderState.animateScrollToItem(it) }
                }
                Orientation.Horizontal -> {
                    horizontalReaderState.animateScrollToPage(
                        horizontalReaderState.currentPage + 1
                    )
                }
            }
        }
    }
}