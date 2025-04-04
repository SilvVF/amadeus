package io.silv.reader2

import android.app.DownloadManager
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import android.view.MotionEvent
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Animation
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.internal.rememberComposableLambda
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.core.graphics.withScale
import androidx.core.graphics.withTranslation
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.LifecycleResumeEffect
import cafe.adriel.voyager.core.lifecycle.DisposableEffectIgnoringConfiguration
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import io.silv.common.DependencyAccessor
import io.silv.common.commonDeps
import io.silv.common.model.Page
import io.silv.di.downloadDeps
import io.silv.reader.ReaderScreenModel
import io.silv.reader.Viewer
import io.silv.reader.loader.ReaderChapter
import io.silv.reader.loader.ReaderPage
import io.silv.ui.R
import io.silv.ui.ScreenStateHandle
import io.silv.ui.collectEvents
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.Serializable
import kotlin.math.abs

class ReaderScreen2(
    mangaId: String,
    val chapterId: String,
) : Screen {

    val saved = ScreenStateHandle(mapOf("manga_id" to mangaId)).apply {
        this["chapter_id"] = get<String>("chapter_id") ?: chapterId
    }

    override val key: ScreenKey = "ReaderScreen_${mangaId}_${saved.get<String>("chapter_id")}"

    @Composable
    override fun Content() {

        SavedStateHandle
        val screenModel = rememberScreenModel { Reader2ScreenModel(saved) }

        LifecycleResumeEffect(screenModel) {
            screenModel.restartReadTimer()
            onPauseOrDispose {
                screenModel.flushReadTimer()
            }
        }

        screenModel.collectEvents { event ->
            when(event) {
                is Reader2ScreenModel.Event.CopyImage -> TODO()
                Reader2ScreenModel.Event.PageChanged -> TODO()
                Reader2ScreenModel.Event.ReloadViewerChapters -> TODO()
                is Reader2ScreenModel.Event.SavedImage -> TODO()
                is Reader2ScreenModel.Event.SetCoverResult -> TODO()
                is Reader2ScreenModel.Event.SetOrientation -> TODO()
                is Reader2ScreenModel.Event.ShareImage -> TODO()
            }
        }

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


abstract class PagerViewer(
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



