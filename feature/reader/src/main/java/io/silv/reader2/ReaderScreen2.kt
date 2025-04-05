package io.silv.reader2

import android.R
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil.decode.ImageSource
import coil.request.CachePolicy
import coil.request.ImageRequest
import io.silv.common.DependencyAccessor
import io.silv.common.coroutine.suspendRunCatching
import io.silv.common.log.LogPriority
import io.silv.common.log.asLog
import io.silv.common.log.logcat
import io.silv.common.model.Page
import io.silv.data.util.ImageUtil
import io.silv.data.util.ImageUtil.splitInHalf
import io.silv.di.downloadDeps
import io.silv.reader.InsertPage
import io.silv.reader.Viewer
import io.silv.reader.ViewerChapters
import io.silv.reader.loader.ReaderChapter
import io.silv.reader.loader.ReaderPage
import io.silv.ui.LocalAppState
import io.silv.ui.ScreenStateHandle
import io.silv.ui.collectEvents
import io.silv.ui.composables.CircularProgressIndicator
import io.silv.ui.rememberLambda
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import me.saket.telephoto.zoomable.ZoomableImageState
import me.saket.telephoto.zoomable.ZoomableState
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import okio.Buffer
import okio.BufferedSource

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
            when (event) {
                is Reader2ScreenModel.Event.CopyImage -> TODO()
                Reader2ScreenModel.Event.PageChanged -> displayRefreshHost.flash()
                Reader2ScreenModel.Event.ReloadViewerChapters -> {
                    screenModel.state.value.viewerChapters?.let(setChapters)
                }

                is Reader2ScreenModel.Event.SavedImage -> TODO()
                is Reader2ScreenModel.Event.SetCoverResult -> TODO()
                is Reader2ScreenModel.Event.SetOrientation -> setOrientation(event.orientation)
                is Reader2ScreenModel.Event.ShareImage -> TODO()
            }
        }

        val state by screenModel.state.collectAsStateWithLifecycle()

        ReaderScreenContent(state)
    }
}

@Composable
private fun ReaderScreenContent(state: Reader2ScreenModel.State) {
    ReaderDialogHost(Modifier.fillMaxSize()) {
        ReaderNavigationOverlay(
            state = rememberReaderOverlayState(),
            modifier = Modifier.fillMaxSize()
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
) : Viewer {

    @OptIn(DependencyAccessor::class)
    val downloadManager = downloadDeps.downloadManager

    var currentPage: Any? = null
    var currentChapter: ReaderChapter? = null

    private val scope = MainScope()

    val config: PagerConfig = PagerConfig(this, scope)

    val pagerState = PagerState { pages.size }

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

    fun onPageSplit(page: ReaderPage, newPage: ReaderPage) {

    }
}

class PagerPageHolder(
    val viewer: PagerViewer,
    val page: ReaderPage,
    val context: Context,
    scope: CoroutineScope
) {
    val status = page.statusFlow.stateIn(
        scope,
        SharingStarted.WhileSubscribed(5_000),
        initialValue = Page.State.QUEUE
    )

    var image by mutableStateOf<ImageRequest?>(null)

    init {
        scope.launch(Dispatchers.IO) {
            val loader = page.chapter.pageLoader ?: return@launch
            loader.loadPage(page)
        }

        scope.launch {
            status.filter { it == Page.State.READY }.collectLatest {
                val streamFn = page.stream ?: return@collectLatest

                suspendRunCatching {
                    val (source, isAnimated, background) = withContext(Dispatchers.IO) {
                        val source = streamFn().use { ins ->
                            process(page, Buffer().readFrom(ins))
                        }
                        val isAnimated = ImageUtil.isAnimatedAndSupported(source)
                        val background = if (!isAnimated && viewer.config.automaticBackground) {
                            ImageUtil.chooseBackground(context, source.peek().inputStream())
                        } else {
                            null
                        }
                        Triple(source, isAnimated, background)
                    }
                    image = coilImageRequest(source, isAnimated, background)
                }
            }
        }
    }

    private fun coilImageRequest(
        source: BufferedSource,
        isAnimated: Boolean,
        background: Drawable?
    ): ImageRequest {
        return if (isAnimated) {
            TODO("animated image")
        } else {
            ImageRequest.Builder(context)
                .data(source.readByteArray())
                .crossfade(true)
                .build()
        }
    }

    private fun process(page: ReaderPage, imageSource: BufferedSource): BufferedSource {
        if (viewer.config.dualPageRotateToFit) {
            return rotateDualPage(imageSource)
        }

        if (!viewer.config.dualPageSplit) {
            return imageSource
        }

        if (page is InsertPage) {
            return splitInHalf(imageSource)
        }

        val isDoublePage = ImageUtil.isWideImage(imageSource)
        if (!isDoublePage) {
            return imageSource
        }

        onPageSplit(page)

        return splitInHalf(imageSource)
    }

    private fun rotateDualPage(imageSource: BufferedSource): BufferedSource {
        val isDoublePage = ImageUtil.isWideImage(imageSource)
        return if (isDoublePage) {
            val rotation = if (viewer.config.dualPageRotateToFitInvert) -90f else 90f
            ImageUtil.rotateImage(imageSource, rotation)
        } else {
            imageSource
        }
    }

    private fun splitInHalf(imageSource: BufferedSource): BufferedSource {
        var side = when {
            true -> ImageUtil.Side.RIGHT
//            viewer is L2RPagerViewer && page is InsertPage -> ImageUtil.Side.RIGHT
//            viewer !is L2RPagerViewer && page is InsertPage -> ImageUtil.Side.LEFT
//            viewer is L2RPagerViewer && page !is InsertPage -> ImageUtil.Side.LEFT
//            viewer !is L2RPagerViewer && page !is InsertPage -> ImageUtil.Side.RIGHT
            else -> error("We should choose a side!")
        }

        if (viewer.config.dualPageInvert) {
            side = when (side) {
                ImageUtil.Side.RIGHT -> ImageUtil.Side.LEFT
                ImageUtil.Side.LEFT -> ImageUtil.Side.RIGHT
            }
        }

        return splitInHalf(imageSource, side)
    }

    private fun onPageSplit(page: ReaderPage) {
        val newPage = InsertPage(page)
        viewer.onPageSplit(page, newPage)
    }
}

@Composable
private fun PagerPage(
    pagerPageHolder: PagerPageHolder,
    modifier: Modifier = Modifier,
    zoomState: ZoomableImageState = rememberZoomableImageState()
) {
    val status by pagerPageHolder.status.collectAsStateWithLifecycle()
    Box(modifier) {
        when (status) {
            Page.State.QUEUE, Page.State.LOAD_PAGE, Page.State.DOWNLOAD_IMAGE -> {
                androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            Page.State.READY -> {
                ZoomableAsyncImage(
                    model = pagerPageHolder.image,
                    contentDescription = pagerPageHolder.page.url,
                    state = zoomState,
                    onClick = {

                    }
                )
            }
            Page.State.ERROR -> Text("error", Modifier.align(Alignment.Center))
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ComposeViewPager(
    pagerViewer: PagerViewer,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val readerPage = @Composable { page: ReaderPage ->
        val holder = remember {
            PagerPageHolder(pagerViewer, page, context, scope)
        }
        PagerPage(holder)
    }

    Box(modifier) {
        if (pagerViewer.isHorizontal) {
            HorizontalPager(
                pagerViewer.pagerState,
                beyondViewportPageCount = 1
            ) {
                val page = pagerViewer.pages[it]
                when (page) {
                    is ReaderPage -> readerPage(page)
                }
            }
        } else {
            VerticalPager(
                pagerViewer.pagerState,
                beyondViewportPageCount = 1
            ) {
                val page = pagerViewer.pages[it]

                when (page) {
                    is ReaderPage -> readerPage(page)
                }
            }
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
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val viewer = remember {
        PagerViewer(context, haptics, emptyList())
    }

    ComposeViewPager(viewer)
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



