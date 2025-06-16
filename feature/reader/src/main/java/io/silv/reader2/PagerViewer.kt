package io.silv.reader2

import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import io.silv.common.log.logcat
import io.silv.data.chapter.Chapter
import io.silv.reader.loader.ChapterTransition
import io.silv.reader.loader.ReaderChapter
import io.silv.reader.loader.ReaderPage
import io.silv.reader.loader.ViewerPage
import io.silv.reader2.ViewerNavigation.NavigationRegion
import io.silv.ui.EventScreenModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.floor

sealed interface PagerAction {
    data object ToggleMenu : PagerAction
    data object ShowMenu : PagerAction
    data class OnPageSelected(val page: ReaderPage) : PagerAction
    data class RequestPreloadChapter(val chapter: ReaderChapter) : PagerAction
}

class PagerViewer(
    val isHorizontal: Boolean = true,
    val l2r: Boolean = true,
    val scope: CoroutineScope,
    val onAction: (PagerAction) -> Unit
) : Viewer {

    sealed interface UiEvent {
        data class AnimateScrollToPage(val page: Int) : UiEvent
    }

    val config: PagerConfig = PagerConfig(this, scope)
    private var preprocessed: MutableMap<Int, InsertPage> = mutableMapOf()

    var nextTransition: ChapterTransition.Next? = null
        private set

    var items = mutableStateListOf<ViewerPage>()
        private set
    var currentPage by mutableStateOf<ViewerPage?>(null)
        private set
    var currentChapter by mutableStateOf<ReaderChapter?>(null)
        private set

    val pagerState = PagerState { items.size }

    val uiEvents = Channel<UiEvent>(UNLIMITED)

    private fun animateScrollToPage(page: Int) {
        uiEvents.trySend(
            UiEvent.AnimateScrollToPage(page)
        )
    }

    val totalPages by derivedStateOf {
        currentChapter?.pages?.size ?: -1
    }

    val currentPageNumber by derivedStateOf {
        val pages = items
            .filterIsInstance<ReaderPage>()
            .filter { it.chapter.chapter == currentChapter?.chapter }
            .filterNot { it is InsertPage }

        pages.indexOfFirst {
            it == when (val page = currentPage) {
                is InsertPage -> page.parent
                is ReaderPage -> page
                else -> null
            }
        } + 1
    }

    init {
        snapshotFlow { pagerState.settledPage }
            .onEach {
                handleSettledPage(it)
            }
            .launchIn(scope)
    }

    private fun handleSettledPage(idx: Int) {
        val page = items.getOrNull(idx)
        if (page != null && currentPage != page) {
            val allowPreload = checkAllowPreload(page as? ReaderPage)
            val forward = when {
                currentPage is ReaderPage && page is ReaderPage -> {
                    // if both pages have the same number, it's a split page with an InsertPage
                    if (page.number == (currentPage as ReaderPage).number) {
                        // the InsertPage is always the second in the reading direction
                        page is InsertPage
                    } else {
                        page.number > (currentPage as ReaderPage).number
                    }
                }

                currentPage is ChapterTransition.Prev && page is ReaderPage -> false
                else -> true
            }
            when (page) {
                is ReaderPage -> {
                    onReaderPageSelected(page, allowPreload, forward)
                    currentChapter = page.chapter
                }

                is ChapterTransition -> onTransitionSelected(page)
            }
            currentPage = page
        }
    }

    /**
     * Called when a [ChapterTransition] is marked as active. It request the
     * preload of the destination chapter of the transition.
     */
    private fun onTransitionSelected(transition: ChapterTransition) {
        logcat { "onTransitionSelected: to = ${transition.to} from = ${transition.from}" }
        val toChapter = transition.to
        if (toChapter != null) {
            logcat { "Request preload destination chapter because we're on the transition" }
            onAction(PagerAction.RequestPreloadChapter(toChapter))
        } else if (transition is ChapterTransition.Next) {
            // No more chapters, show menu because the user is probably going to close the reader
            onAction(PagerAction.ShowMenu)
        }
    }

    /**
     * Called when a [ReaderPage] is marked as active. It notifies the
     * activity of the change and requests the preload of the next chapter if this is the last page.
     */
    private fun onReaderPageSelected(page: ReaderPage, allowPreload: Boolean, forward: Boolean) {
        val pages = page.chapter.pages ?: return
        logcat { "onReaderPageSelected: ${page.number}/${pages.size}" }
        onAction(PagerAction.OnPageSelected(page))
        // Skip preload on inserts it causes unwanted page jumping
        if (page is InsertPage) {
            return
        }
        // Preload next chapter once we're within the last 5 pages of the current chapter
        val inPreloadRange = pages.size - page.number < 5
        if (inPreloadRange && allowPreload && page.chapter == currentChapter) {
            logcat { "Request preload next chapter because we're at page ${page.number} of ${pages.size}" }
            nextTransition?.to?.let {
                onAction(PagerAction.RequestPreloadChapter(it))
            }
        }
    }


    private fun checkAllowPreload(page: ReaderPage?): Boolean {
        // Page is transition page - preload allowed
        page ?: return true

        // Initial opening - preload allowed
        currentPage ?: return true

        // Allow preload for
        // 1. Going to next chapter from chapter transition
        // 2. Going between pages of same chapter
        // 3. Next chapter page
        return when (page.chapter) {
            (currentPage as? ChapterTransition.Next)?.to -> true
            (currentPage as? ReaderPage)?.chapter -> true
            nextTransition?.to -> true
            else -> false
        }
    }

    private fun calculateChapterGap(higherChapter: Chapter?, lowerChapter: Chapter?): Int {
        if (higherChapter == null || lowerChapter == null) return 0
        if (higherChapter.chapter < 0 || lowerChapter.chapter < 0) return 0
        return calculateChapterGap(higherChapter.chapter, lowerChapter.chapter)
    }

    private fun calculateChapterGap(higherChapterNumber: Double, lowerChapterNumber: Double): Int {
        if (higherChapterNumber < 0.0 || lowerChapterNumber < 0.0) return 0
        return floor(higherChapterNumber).toInt() - floor(lowerChapterNumber).toInt() - 1
    }

    override fun setChapters(chapters: ViewerChapters) {
        logcat { "setChapters curr = ${chapters.currChapter}, next = ${chapters.nextChapter}, prev = ${chapters.prevChapter}" }
        val newItems = mutableListOf<ViewerPage>()

        // Forces chapter transition if there is missing chapters
        val prevHasMissingChapters =
            calculateChapterGap(chapters.currChapter.chapter, chapters.prevChapter?.chapter) > 0
        val nextHasMissingChapters =
            calculateChapterGap(chapters.nextChapter?.chapter, chapters.currChapter.chapter) > 0

        // Add previous chapter pages and transition.
        if (chapters.prevChapter != null) {
            // We only need to add the last few pages of the previous chapter, because it'll be
            // selected as the current chapter when one of those pages is selected.
            val prevPages = chapters.prevChapter.pages
            if (prevPages != null) {
                newItems.addAll(prevPages.takeLast(2))
            }
        }

        // Skip transition page if the chapter is loaded & current page is not a transition page
        if (prevHasMissingChapters || chapters.prevChapter?.state !is ReaderChapter.State.Loaded) {
            newItems.add(ChapterTransition.Prev(chapters.currChapter, chapters.prevChapter))
        }

        var insertPageLastPage: InsertPage? = null

        // Add current chapter.
        val currPages = chapters.currChapter.pages
        if (currPages != null) {
            val pages = currPages.toMutableList()

            val lastPage = pages.last()

            // Insert preprocessed pages into current page list
            preprocessed.keys.sortedDescending()
                .forEach { key ->
                    if (lastPage.index == key) {
                        insertPageLastPage = preprocessed[key]
                    }
                    preprocessed[key]?.let { pages.add(key + 1, it) }
                }

            newItems.addAll(pages)
        }

        // Add next chapter transition and pages.
        nextTransition = ChapterTransition.Next(chapters.currChapter, chapters.nextChapter)
            .also {
                if (nextHasMissingChapters ||
                    chapters.nextChapter?.state !is ReaderChapter.State.Loaded
                ) {
                    newItems.add(it)
                }
            }

        if (chapters.nextChapter != null) {
            // Add at most two pages, because this chapter will be selected before the user can
            // swap more pages.
            val nextPages = chapters.nextChapter.pages
            if (nextPages != null) {
                newItems.addAll(nextPages.take(2))
            }
        }

        preprocessed = mutableMapOf()

        Snapshot.withMutableSnapshot {
            items.clear()
            items.addAll(newItems)
        }

        // Will skip insert page otherwise
        insertPageLastPage?.let { moveToPage(it) }
    }

    override fun moveToPage(page: ReaderPage) {
        val idx = items.indexOfFirst { it == page }.takeIf { it != -1 } ?: return
        scope.launch {
            uiEvents.trySend(
                UiEvent.AnimateScrollToPage(idx)
            )
        }
    }

    fun handleClickEvent(offset: Offset, size: IntSize) {
        logcat { "$offset" }
        when (config.navigator.getAction(offset, size).also {
            logcat { "$it" }
        }) {
            NavigationRegion.MENU -> onAction(PagerAction.ToggleMenu)
            NavigationRegion.NEXT -> if (l2r || !isHorizontal) {
                animateScrollToPage(pagerState.currentPage + 1)
            } else {
                animateScrollToPage(pagerState.currentPage - 1)
            }

            NavigationRegion.PREV -> if (l2r || !isHorizontal) {
                animateScrollToPage(pagerState.currentPage - 1)
            } else {
                animateScrollToPage(pagerState.currentPage + 1)
            }

            NavigationRegion.RIGHT -> animateScrollToPage(pagerState.currentPage + 1)
            NavigationRegion.LEFT -> animateScrollToPage(pagerState.currentPage - 1)
        }
    }

    fun cleanupPageSplit() {
        val insertPages = items.filterIsInstance<InsertPage>()
        items.removeAll(insertPages)
    }

    fun onPageSplit(currentPage: Any?, newPage: InsertPage) {
        if (currentPage !is ReaderPage) return

        val currentIndex = items.indexOf(currentPage)

        // Put aside preprocessed pages for next chapter so they don't get removed when changing chapter
        if (currentPage.chapter.chapter.id != currentChapter?.chapter?.id) {
            preprocessed[newPage.index] = newPage
            return
        }
        val placeAtIndex = if (!isHorizontal || l2r) {
            currentIndex + 1
        } else {
            currentIndex
        }

        // It will enter a endless cycle of insert pages
        if (!l2r && placeAtIndex - 1 >= 0 && items[placeAtIndex - 1] is InsertPage) {
            return
        }

        // Same here it will enter a endless cycle of insert pages
        if (items[placeAtIndex] is InsertPage) {
            return
        }

        items.add(placeAtIndex, newPage)
    }
}
