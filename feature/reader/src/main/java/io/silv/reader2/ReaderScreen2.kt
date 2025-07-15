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
import io.silv.common.model.ReaderLayout
import io.silv.reader.composables.ChapterActions
import io.silv.reader.composables.ReaderMenuOverlay
import io.silv.reader.loader.ChapterTransition
import io.silv.reader.loader.ReaderChapter
import io.silv.reader.loader.ReaderPage
import io.silv.reader.loader.ViewerPage
import io.silv.ui.LocalAppState
import io.silv.ui.collectEvents
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
        val navigator = LocalNavigator.currentOrThrow
        val lifecycleOwner = LocalLifecycleOwner.current

        val screenModel = rememberScreenModel {
            Reader2ScreenModel(
                chapterId = chapterId,
                chapterPageIndex = page,
                mangaId = mangaId
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


        val displayRefreshHost = remember { DisplayRefreshHost() }

        screenModel.collectEvents { event ->
            when (event) {
                is Reader2ScreenModel.Event.CopyImage -> TODO()
                Reader2ScreenModel.Event.PageChanged -> displayRefreshHost.flash()
                is Reader2ScreenModel.Event.ShareImage -> TODO()
            }
        }

        val scope = rememberCoroutineScope()

        val animateToFirstPage = {
            scope.launch {
                with(screenModel.viewer) {
                    val idx = items.indexOfFirst {
                        when (it) {
                            is ChapterTransition -> false
                            is ReaderPage -> it.chapter.chapter.id == viewerChapters?.currChapter?.chapter?.id && it.number == 1
                        }
                    }
                    if (idx != -1) pagerState.animateScrollToPage(idx)
                }
            }
        }

        ReaderScreenContent(
            state = state,
            viewer = screenModel.viewer,
            loadPrevChapter = {
                lifecycleOwner.lifecycleScope.launch {
                    screenModel.loadPreviousChapter()
                    animateToFirstPage()
                }
            },
            loadNextChapter = {
                lifecycleOwner.lifecycleScope.launch {
                    screenModel.loadNextChapter()
                    animateToFirstPage()
                }
            },
            onBack = {
                navigator.pop()
            },
            onDismiss = {
                screenModel.showMenus(false)
            },
            actions = ChapterActions(
                delete = screenModel::deleteChapterImages,
                markRead = screenModel::toggleChapterRead,
                bookmark = screenModel::toggleChapterBookmark,
                cancelDownload = screenModel::cancelDownload,
                pauseDownloads = screenModel::pauseDownloads,
                download = screenModel::downloadChapterImages
            )
        )
    }
}

@Composable
private fun ReaderScreenContent(
    state: Reader2ScreenModel.State,
    actions: ChapterActions,
    viewer: PagerViewer,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    loadNextChapter: () -> Unit,
    loadPrevChapter: () -> Unit
) {
    ReaderDialogHost(
        Modifier.fillMaxSize(),
        state = state,
        viewer = viewer,
        onDismiss = onDismiss,
        onBack = onBack,
        loadNextChapter = loadNextChapter,
        loadPrevChapter = loadPrevChapter,
        actions = actions
    ) {
        ReaderNavigationOverlay(
            overlayState = rememberReaderOverlayState(viewer.navigator),
            modifier = Modifier.fillMaxSize()
        ) {
            ReaderContainer(viewer)
        }
    }
}

@Composable
fun PageContent(
    page: ViewerPage,
    viewer: PagerViewer
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    when (page) {
        is ReaderPage -> {
            val state = pagePresenter(viewer, context, page)
            PagerPage(
                onPageClick = { offset, size ->
                    scope.launch {
                        viewer.handleClickEvent(offset, size)
                    }
                },
                state
            )
        }

        is ChapterTransition -> {
            var size by remember { mutableStateOf(IntSize.Zero) }
            Column(
                Modifier
                    .fillMaxSize()
                    .onSizeChanged { size = it }
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { offset ->
                            scope.launch {
                                viewer.handleClickEvent(offset, size)
                            }
                        })
                    },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "to: ${page.to?.chapter?.title}",
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

                        ReaderChapter.State.Loading, ReaderChapter.State.Wait -> {
                            CircularProgressIndicator()
                        }

                        is ReaderChapter.State.Loaded -> {
                            Text("Ready")
                        }
                    }
                }
                Text(
                    "from: ${page.from.chapter.title}",
                )
                val from = page.from
                val state by from.stateAsFlow.collectAsState()
                when (state) {
                    is ReaderChapter.State.Error -> {
                        TextButton(
                            onClick = { viewer.retry(from) }
                        ) {
                            Text("Retry")
                        }
                    }

                    ReaderChapter.State.Loading, ReaderChapter.State.Wait -> {
                        CircularProgressIndicator()
                    }

                    is ReaderChapter.State.Loaded -> {
                        Text("Ready")
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

        if (pagerViewer.settings.showPageNumber) {
            PageIndicatorText(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.systemBars),
                currentPage = pagerViewer.currentPageNumber,
                totalPages = pagerViewer.totalPages,
            )
        }
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
) {
    Surface(
        Modifier.fillMaxSize(),
        color = viewer.settings.color
    ) {
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
    actions: ChapterActions,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()

    ReaderMenuOverlay(
        modifier = modifier,
        onDismissRequested = onDismiss,
        onViewOnWebClick = {},
        onBackArrowClick = onBack,
        readerChapter = { viewer.currentChapter },
        manga = { state.manga },
        menuVisible = { state.menuVisible },
        l2r = viewer.l2r,
        chapterActions = actions,
        chapters = state.chapters,
        currentPage = { viewer.currentPageNumber },
        pageCount = { viewer.totalPages },
        loadNextChapter = loadNextChapter,
        loadPrevChapter = loadPrevChapter,
        settings = viewer.settings,
        downloadsProvider = state.downloads,
        changePage = { page ->
            scope.launch {
                val idx = page - 1
                viewer.pagerState.animateScrollToPage(idx.coerceIn(viewer.items.indices))
            }
        }
    ) {
        content()
    }
}



