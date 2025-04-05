package io.silv.reader2

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.silv.common.DependencyAccessor
import io.silv.common.log.LogPriority
import io.silv.common.log.asLog
import io.silv.common.log.logcat
import io.silv.di.downloadDeps
import io.silv.reader.Viewer
import io.silv.reader.ViewerChapters
import io.silv.reader.loader.ReaderChapter
import io.silv.reader.loader.ReaderPage
import io.silv.ui.LocalAppState
import io.silv.ui.ScreenStateHandle
import io.silv.ui.collectEvents
import io.silv.ui.rememberLambda
import kotlinx.coroutines.MainScope

private fun requireActivity(context: Context): ComponentActivity {
    return when (context) {
        is ComponentActivity -> context
        is ContextWrapper -> requireActivity(context.baseContext)
        else -> error("no activity")
    }
}

data class ReaderScreen2(
    val mangaId: String,
    val chapterId: String,
) : Screen {

    val saved = ScreenStateHandle(mapOf("manga_id" to mangaId))

    override val key: ScreenKey = "ReaderScreen_${mangaId}_${saved.get<String>("chapter_id")}"

    @Composable
    override fun Content() {

        val appState = LocalAppState.current
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val screenModel = rememberScreenModel { Reader2ScreenModel(saved, appState) }

        LaunchedEffect(Unit) {
            val initResult = screenModel.init(mangaId, chapterId)

            if (!initResult.getOrDefault(false)) {
                val exception = initResult.exceptionOrNull() ?: IllegalStateException(
                    "Unknown err",
                )
                logcat(LogPriority.ERROR) { exception.asLog() }
                appState.showSnackBar(exception.message.orEmpty())
                navigator.pop()
            }
        }

        LifecycleResumeEffect(screenModel) {
            screenModel.restartReadTimer()
            onPauseOrDispose {
                screenModel.flushReadTimer()
            }
        }

        val setChapters = rememberLambda { viewerChapters: ViewerChapters ->
            screenModel.state.value.viewer?.setChapters(viewerChapters)
        }

        val setOrientation = rememberLambda { orientation: Int ->
            val newOrientation = Reader2Orientation.fromPreference(orientation)
            val activity = requireActivity(context)
            if (newOrientation.flag != activity.requestedOrientation) {
                activity.requestedOrientation = newOrientation.flag
            }
        }

        val displayRefreshHost = remember { DisplayRefreshHost() }

        screenModel.collectEvents { event ->
            when(event) {
                is Reader2ScreenModel.Event.CopyImage -> TODO()
                Reader2ScreenModel.Event.PageChanged -> displayRefreshHost.flash()
                Reader2ScreenModel.Event.ReloadViewerChapters -> {
                    screenModel.state.value.viewerChapters?.let(setChapters)
                }
                is Reader2ScreenModel.Event.SavedImage -> TODO()
                is Reader2ScreenModel.Event.SetCoverResult -> TODO()
                is Reader2ScreenModel.Event.SetOrientation ->  setOrientation(event.orientation)
                is Reader2ScreenModel.Event.ShareImage -> TODO()
            }
        }

        val state by screenModel.state.collectAsStateWithLifecycle()

        ReaderScreenContent(state)
    }
}

@Composable
private fun ReaderScreenContent(state: Reader2ScreenModel.State) {
    ReaderDialogHost {
        ReaderNavigationOverlay(
            state = rememberReaderOverlayState()
        ) {
            ReaderContainer(state)
        }
    }
}


class PagerViewer(
    context: Context,
    feedback: HapticFeedback,
    val pages: List<Any?>,
    val isHorizontal: Boolean = true,
): Viewer {

    @OptIn(DependencyAccessor::class)
    val downloadManager = downloadDeps.downloadManager

    var currentPage: Any? = null
    var currentChapter: ReaderChapter? = null

    private val scope = MainScope()
    val pagerState = PagerState { pages.size }

    val beyondBoundsPageCount = 1

    val modifier = Modifier
        .fillMaxSize()
        .focusable(false)

    var nextTransition: ChapterTransition.Next? = null
        private set


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

    @Composable
    private fun PageRenderer(position: Int) {
        val page = pages.getOrNull(position)
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

                currentPage is ChapterTransition.Prev && page is ReaderPage ->
                    false

                else -> true
            }
            currentPage = page
            when (page) {
                is ReaderPage -> onReaderPageSelected(page, allowPreload, forward)
                is ChapterTransition -> onTransitionSelected(page)
            }
        }
    }

    override fun setChapters(chapters: ViewerChapters) {
        TODO("Not yet implemented")
    }

    override fun moveToPage(page: ReaderPage) {
        TODO("Not yet implemented")
    }

    override fun handleKeyEvent(event: KeyEvent): Boolean {
        TODO("Not yet implemented")
    }

    override fun handleGenericMotionEvent(event: MotionEvent): Boolean {
        TODO("Not yet implemented")
    }

    @Composable
    override fun Content() {
        ComposeViewPager(this)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ComposeViewPager(
    pagerViewer: PagerViewer,
    modifier: Modifier = Modifier
) {

    if (pagerViewer.isHorizontal) {
        HorizontalPager(
            pagerViewer.pagerState,
            beyondViewportPageCount = pagerViewer.beyondBoundsPageCount
        ) {

        }
    } else {
        VerticalPager(
            pagerViewer.pagerState,
            beyondViewportPageCount = pagerViewer.beyondBoundsPageCount
        ) {

        }
    }
}

sealed class ChapterTransition {

    abstract val from: ReaderChapter
    abstract val to: ReaderChapter?

    class Prev(
        override val from: ReaderChapter,
        override val to: ReaderChapter?,
    ) : ChapterTransition()

    class Next(
        override val from: ReaderChapter,
        override val to: ReaderChapter?,
    ) : ChapterTransition()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChapterTransition) return false
        if (from == other.from && to == other.to) return true
        if (from == other.to && to == other.from) return true
        return false
    }

    override fun hashCode(): Int {
        var result = from.hashCode()
        result = 31 * result + (to?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "${javaClass.simpleName}(from=${from.chapter.url}, to=${to?.chapter?.url})"
    }
}

@Composable
fun ReaderContainer(
    state: Reader2ScreenModel.State
) {
    state.viewer?.Content()
}

@Composable
fun ReaderDialogHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier) {
        content()
    }
}



