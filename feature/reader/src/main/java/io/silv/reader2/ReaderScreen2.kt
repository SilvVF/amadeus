package io.silv.reader2

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.silv.common.log.LogPriority
import io.silv.common.log.asLog
import io.silv.common.log.logcat
import io.silv.reader.composables.ChapterActions
import io.silv.reader.composables.ReaderMenuOverlay
import io.silv.reader.loader.ChapterTransition
import io.silv.reader.loader.ReaderChapter
import io.silv.reader.loader.ReaderPage
import io.silv.reader.loader.ViewerPage
import io.silv.ui.LocalAppState
import io.silv.ui.collectEvents
import io.silv.ui.rememberLambda
import kotlinx.coroutines.launch

private fun requireActivity(context: Context): ComponentActivity {
    return when (context) {
        is ComponentActivity -> context
        is ContextWrapper -> requireActivity(context.baseContext)
        else -> error("no activity")
    }
}

data class ReaderScreen2(
    val mangaId: String,
    var chapterId: String,
) : Screen {

    var page: Int = 0

    override val key: ScreenKey get() = "${mangaId}_$chapterId"

    @Composable
    override fun Content() {

        val appState = LocalAppState.current
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val lifecycleOwner = LocalLifecycleOwner.current

        val screenModel = rememberScreenModel {
            Reader2ScreenModel(
                chapterId = chapterId,
                chapterPageIndex = page,
                mangaId = mangaId,
                appState = appState
            ) { state ->
                page = (state[Reader2ScreenModel.PAGE_KEY] as? Int) ?: 0
                chapterId = (state[Reader2ScreenModel.CHAPTER_KEY] as? String) ?: chapterId
            }
        }
        val state by screenModel.state.collectAsStateWithLifecycle()

        LaunchedEffect(screenModel) {
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
                is Reader2ScreenModel.Event.SetOrientation -> setOrientation(event.orientation)
                is Reader2ScreenModel.Event.ShareImage -> TODO()
            }
        }


        screenModel.viewer.uiEvents.collectEvents { event ->
            when (event) {
                is PagerEvent.AnimateScrollToPage -> {
                    screenModel.viewer.pagerState.animateScrollToPage(event.page)
                }
            }
        }

        ReaderScreenContent(
            state,
            screenModel.viewer,
            loadPrevChapter = {
                lifecycleOwner.lifecycleScope.launch {
                    screenModel.loadPreviousChapter()
                }
            },
            loadNextChapter = {
                lifecycleOwner.lifecycleScope.launch {
                    screenModel.loadNextChapter()
                }
            },
            onBack = {
                navigator.pop()
            },
            onDismiss = {
                screenModel.showMenus(false)
            }
        )
    }
}

@Composable
private fun ReaderScreenContent(
    state: Reader2ScreenModel.State,
    viewer: PagerViewer,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    loadNextChapter: () -> Unit,
    loadPrevChapter: () -> Unit
) {
    ReaderDialogHost(
        Modifier.fillMaxSize(),
        state,
        viewer,
        onDismiss = onDismiss,
        onBack = onBack,
        loadNextChapter = loadNextChapter,
        loadPrevChapter = loadPrevChapter,
    ) {
        ReaderNavigationOverlay(
            overlayState = rememberReaderOverlayState(viewer.config.navigator),
            modifier = Modifier.fillMaxSize()
        ) {
            ReaderContainer(viewer, state)
        }
    }
}

@Composable
fun PageContent(
    page: ViewerPage,
    viewer: PagerViewer
) {
    val context = LocalContext.current
    when (page) {
        is ReaderPage -> {
            val state = pagePresenter(viewer, context, page)
            PagerPage(state)
        }

        is ChapterTransition -> {
            var size by remember { mutableStateOf(IntSize.Zero) }
            Column(
                Modifier
                    .fillMaxSize()
                    .onSizeChanged { size = it }
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            viewer.handleClickEvent(it, size)
                        })
                    },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "to: ${page.to?.chapter?.title} from: ${page.from.chapter.title}",
                )
                val to = page.to
                if (to != null) {
                    val state by to.stateAsFlow.collectAsState()
                    when (state) {
                        is ReaderChapter.State.Error -> {
                            TextButton(
                                onClick = { viewer.retry(to) }
                            ) {
                                Text("Retry")
                            }
                        }
                        ReaderChapter.State.Loading -> {
                            CircularProgressIndicator()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}

@Composable
fun ComposeViewPager(
    pagerViewer: PagerViewer,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        if (pagerViewer.isHorizontal) {
            HorizontalPager(
                pagerViewer.pagerState,
                beyondViewportPageCount = 1,
                reverseLayout = !pagerViewer.l2r,
                modifier = Modifier.fillMaxSize(),
                key = { pagerViewer.items[it].hashCode() }
            ) {
                val page = pagerViewer.items[it]
                PageContent(page, pagerViewer)
            }
        } else {
            VerticalPager(
                pagerViewer.pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxSize(),
                key = { pagerViewer.items[it].hashCode() }
            ) {
                val page = pagerViewer.items[it]
                PageContent(page, pagerViewer)
            }
        }
        PageIndicatorText(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.systemBars),
            currentPage = pagerViewer.currentPageNumber,
            totalPages = pagerViewer.totalPages,
        )
    }
}


@Composable
private fun PageIndicatorText(
    modifier: Modifier = Modifier,
    currentPage: Int,
    totalPages: Int,
) {
    if (currentPage <= 0 || totalPages <= 0) return

    val text = "$currentPage / $totalPages"

    val style = TextStyle(
        color = Color(235, 235, 235),
        fontSize = MaterialTheme.typography.bodySmall.fontSize,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
    )
    val strokeStyle = style.copy(
        color = Color(45, 45, 45),
        drawStyle = Stroke(width = 4f),
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = strokeStyle,
        )
        Text(
            text = text,
            style = style,
        )
    }
}

@Composable
fun ReaderContainer(
    viewer: PagerViewer,
    state: Reader2ScreenModel.State
) {
    Surface(Modifier.fillMaxSize()) {
        ComposeViewPager(viewer)
    }
}

@Composable
fun ReaderDialogHost(
    modifier: Modifier = Modifier,
    state: Reader2ScreenModel.State,
    viewer: PagerViewer,
    loadPrevChapter: () -> Unit,
    loadNextChapter: () -> Unit,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    ReaderMenuOverlay(
        onDismissRequested = onDismiss,
        onViewOnWebClick = {},
        onBackArrowClick = onBack,
        readerChapter = { viewer.currentChapter },
        manga = { state.manga },
        menuVisible = { state.menuVisible },
        layoutDirection = if (viewer.l2r) LayoutDirection.Ltr else LayoutDirection.Rtl,
        chapterActions = ChapterActions(),
        chapters = { state.chapters },
        currentPage = { viewer.currentPageNumber },
        pageCount = { viewer.totalPages },
        loadNextChapter = loadNextChapter,
        loadPrevChapter = loadPrevChapter,
        settings = state.settings,
        changePage = { page ->
            scope.launch {
                val idx = page - 1
                viewer.pagerState.animateScrollToPage(idx.coerceAtLeast(0))
            }
        }
    ) {
        content()
    }
}



