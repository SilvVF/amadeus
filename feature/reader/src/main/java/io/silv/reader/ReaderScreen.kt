@file:OptIn(ExperimentalFoundationApi::class)

package io.silv.reader

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil.request.ImageRequest
import io.silv.common.model.Page
import io.silv.datastore.ReaderPrefs
import io.silv.datastore.collectAsState
import io.silv.domain.chapter.model.Chapter
import io.silv.domain.manga.model.Manga
import io.silv.reader.composables.ChapterActions
import io.silv.reader.composables.GestureHintOverlay
import io.silv.reader.composables.ReaderMenuOverlay
import io.silv.reader.loader.ReaderChapter
import io.silv.reader.loader.ReaderPage
import io.silv.ui.CenterBox
import io.silv.ui.Converters
import io.silv.ui.ReaderLayout
import io.silv.ui.conditional
import io.silv.ui.openOnWeb
import io.silv.ui.theme.LocalSpacing
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import org.koin.core.parameter.parametersOf
import java.nio.ByteBuffer
import kotlin.math.roundToInt

class ReaderScreen(
    val mangaId: String,
    val chapterId: String,
) : Screen {

    override val key: ScreenKey = "ReaderScreen_${mangaId}_${chapterId}"

    @Stable
    private var savedStateChapterId = chapterId

    @Composable
    override fun Content() {

        val screenModel = getScreenModel<ReaderScreenModel> { parametersOf(mangaId, savedStateChapterId) }

        val state = screenModel.state.collectAsStateWithLifecycle().value
        val lifecycle = LocalLifecycleOwner.current

        DisposableEffect(lifecycle.lifecycle) {

            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        screenModel.initialChapterId = savedStateChapterId
                        screenModel.mangaId = mangaId
                        screenModel.initializeReader()
                        screenModel.restartReadTimer()
                    }
                    Lifecycle.Event.ON_PAUSE -> screenModel.flushReadTimer()
                    else -> Unit
                }
            }

            lifecycle.lifecycle.addObserver(
                observer = observer
            )
            onDispose {
                lifecycle.lifecycle.removeObserver(observer)
                screenModel.flushReadTimer()
            }
        }



        LaunchedEffect(key1 = Unit) {
            screenModel.state.map { it.viewerChapters?.currChapter }
                .filterNotNull()
                .distinctUntilChanged()
                .collect {
                    savedStateChapterId = it.chapter.id
                }
        }

        val colors = remember {
            persistentListOf(
                Color.Black to "Black",
                Color.Gray to "Gray",
                Color.White to "White",
                Color.Unspecified to "Default"
            )
        }

        val fullscreen by ReaderPrefs.fullscreen.collectAsState(defaultValue = true)
        val backgroundColor by ReaderPrefs.backgroundColor.collectAsState(defaultValue = 0)
        val layout by ReaderPrefs.layoutDirection.collectAsState(
            defaultValue = ReaderLayout.PagedRTL,
            converter = Converters.LayoutDirectionConverter
        )

        val systemBars = WindowInsets.systemBars

        Box(
            Modifier
                .fillMaxSize()
                .conditional(!fullscreen) {
                    windowInsetsPadding(systemBars)
                }
                .drawWithCache {
                    onDrawBehind {
                        drawRect(
                            color = colors[backgroundColor].first
                        )
                    }
                }
        ) {
            if (state.viewerChapters != null && state.manga != null) {
                when (layout) {
                    ReaderLayout.PagedRTL ->  HorizontalReader(
                        viewerChapters = state.viewerChapters,
                        onPageChange = screenModel::pageChanged,
                        chapterList = state.chapters,
                        loadPrevChapter = screenModel::loadPreviousChapter,
                        loadNextChapter = screenModel::loadNextChapter,
                        manga = state.manga,
                        layoutDirection = LayoutDirection.Rtl,
                        chapterActions = screenModel.chapterActions
                    )
                    ReaderLayout.PagedLTR ->  HorizontalReader(
                        viewerChapters = state.viewerChapters,
                        onPageChange = screenModel::pageChanged,
                        chapterList = state.chapters,
                        loadPrevChapter = screenModel::loadPreviousChapter,
                        loadNextChapter = screenModel::loadNextChapter,
                        manga = state.manga,
                        layoutDirection = LayoutDirection.Ltr,
                        chapterActions = screenModel.chapterActions
                    )
                    ReaderLayout.Vertical -> VerticalReader(
                        viewerChapters = state.viewerChapters,
                        onPageChange = screenModel::pageChanged,
                        chapterList = state.chapters,
                        loadPrevChapter = screenModel::loadPreviousChapter,
                        loadNextChapter = screenModel::loadNextChapter,
                        manga = state.manga,
                        chapterActions = screenModel.chapterActions
                    )
                }
            } else {
                CenterBox(Modifier.fillMaxSize()) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun VerticalReader(
    viewerChapters: ViewerChapters,
    chapterList: ImmutableList<Chapter>,
    manga: Manga,
    onPageChange: (readerChapter: ReaderChapter, page: Page) -> Unit,
    loadNextChapter: () -> Unit,
    loadPrevChapter: () -> Unit,
    chapterActions: ChapterActions,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.currentOrThrow


    val readerChapter = viewerChapters.currChapter

    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = readerChapter.requestedPage)

    LaunchedEffect(readerChapter.chapter) {
        lazyListState.animateScrollToItem(readerChapter.requestedPage)
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }.collect {
            readerChapter.requestedPage = it
            onPageChange(readerChapter, readerChapter.pages?.getOrNull(it) ?: return@collect)
        }
    }

    fun scrollToNextPage() {
        scope.launch {
            lazyListState.animateScrollToItem(
                (lazyListState.firstVisibleItemIndex + lazyListState.layoutInfo.visibleItemsInfo.size)
                    .coerceAtMost(readerChapter.pages?.size ?: 0)
            )
        }
    }

    var menuVisible by remember { mutableStateOf(false) }
    val showPageNumber by ReaderPrefs.showPageNumber.collectAsState(defaultValue = true)
    val pageHeight = LocalConfiguration.current.screenHeightDp.dp

    fun scrollToPage(page: Int) {
        scope.launch {
            lazyListState.animateScrollToItem(page.coerceIn(0..(readerChapter.pages?.size ?: 0)))
        }
    }

    fun scrollToPrevPage() {
        scope.launch {
            lazyListState.animateScrollToItem(
                index = (lazyListState.firstVisibleItemIndex - 1).coerceAtLeast(0),
            )
        }
    }

    val firstVisibleItemIdx by remember(lazyListState) {
        derivedStateOf { lazyListState.firstVisibleItemIndex }
    }


    ReaderMenuOverlay(
        readerChapter =  { readerChapter },
        chapters = { chapterList },
        manga = { manga },
        menuVisible = { menuVisible },
        currentPage = { firstVisibleItemIdx },
        onDismissRequested = { menuVisible = false },
        changePage = ::scrollToPage,
        loadNextChapter = loadNextChapter,
        loadPrevChapter = loadPrevChapter,
        layoutDirection = LayoutDirection.Rtl,
        onBackArrowClick = {
            navigator.pop()
        },
        onViewOnWebClick = {
            context.openOnWeb(
                "https://mangadex.org/chapter/${readerChapter.chapter.id}/${readerChapter.requestedPage}",
                "view chapter on mangadex."
            )
                .onFailure {
                    Toast.makeText(context, "couldn't find a way to open chapter", Toast.LENGTH_SHORT).show()
                }
        },
        chapterActions = chapterActions
    ) {
        val density = LocalDensity.current
        GestureHintOverlay(vertical = true) {
            Box {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalZoneClickable {
                            when (it) {
                                VerticalZone.Top -> scrollToPrevPage()
                                VerticalZone.MIDDLE -> menuVisible = !menuVisible
                                VerticalZone.Bottom -> scrollToNextPage()
                            }
                        }
                ) {
                    items(readerChapter.pages?.size ?: 0, key = { it }) {idx ->
                        readerChapter.pages?.getOrNull(idx)?.let {
                            CenterBox(Modifier.height(pageHeight)) {
                                ReaderPageImageItem(
                                    page = it,
                                    readerChapter = readerChapter,
                                    onClick = {

                                        val pageHeightPx = with(density) { pageHeight.toPx() }
                                        val zoneSplit = pageHeightPx / 3

                                        val zone = when(it.y) {
                                            in 0f..zoneSplit -> VerticalZone.Top
                                            in zoneSplit..(pageHeightPx - zoneSplit) -> VerticalZone.MIDDLE
                                            else -> VerticalZone.Bottom
                                        }
                                        when (zone) {
                                            VerticalZone.Top -> scrollToPrevPage()
                                            VerticalZone.MIDDLE -> menuVisible = !menuVisible
                                            VerticalZone.Bottom -> scrollToNextPage()
                                        }
                                    }
                                )
                            }
                        }
                            ?: CenterBox(modifier = Modifier
                                .fillMaxSize()
                                .height(pageHeight)) {
                                CircularProgressIndicator()
                            }
                    }
                }
                if (showPageNumber) {
                    PageIndicatorText(
                        currentPage = firstVisibleItemIdx + 1,
                        totalPages = readerChapter.pages?.size ?: 0,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.systemBars)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HorizontalReader(
    viewerChapters: ViewerChapters,
    chapterList: ImmutableList<Chapter>,
    manga: Manga,
    onPageChange: (readerChapter: ReaderChapter, page: Page) -> Unit,
    loadNextChapter: () -> Unit,
    loadPrevChapter: () -> Unit,
    layoutDirection: LayoutDirection,
    chapterActions: ChapterActions
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.currentOrThrow

    val readerChapter = viewerChapters.currChapter

    val pagerState =
        rememberPagerState(
            initialPage = readerChapter.requestedPage,
        ) {
            readerChapter.pages?.size ?: 0
        }

    LaunchedEffect(readerChapter.chapter) {
        pagerState.scrollToPage(readerChapter.requestedPage)
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect {
            readerChapter.requestedPage = it
            onPageChange(readerChapter, readerChapter.pages?.getOrNull(it) ?: return@collect)
        }
    }

    fun scrollToNextPage() {
        scope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage + 1)
        }
    }

    fun scrollToPage(page: Int) {
        scope.launch {
            pagerState.animateScrollToPage(page)
        }
    }

    fun scrollToPrevPage() {
        scope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage - 1)
        }
    }

    var menuVisible by remember { mutableStateOf(false) }
    val showPageNumber by ReaderPrefs.showPageNumber.collectAsState(defaultValue = true)

    ReaderMenuOverlay(
        readerChapter =  { readerChapter },
        chapters = { chapterList },
        manga = { manga },
        menuVisible = { menuVisible },
        currentPage = { pagerState.currentPage },
        onDismissRequested = { menuVisible = false },
        changePage = ::scrollToPage,
        loadNextChapter = loadNextChapter,
        loadPrevChapter = loadPrevChapter,
        layoutDirection = layoutDirection,
        onBackArrowClick = {
            navigator.pop()
        },
        onViewOnWebClick = {
              context.openOnWeb(
                  "https://mangadex.org/chapter/${readerChapter.chapter.id}/${readerChapter.requestedPage}",
                  "view chapter on mangadex."
              )
                  .onFailure {
                      Toast.makeText(context, "couldn't find a way to open chapter", Toast.LENGTH_SHORT).show()
                  }
        },
        chapterActions = chapterActions
    ) {
        val density = LocalDensity.current
        val screenWidthDp = LocalConfiguration.current.screenWidthDp
        Box(modifier = Modifier.fillMaxSize()) {
            CompositionLocalProvider(
                LocalLayoutDirection provides layoutDirection
            ) {
                GestureHintOverlay {
                    HorizontalPager(
                        state = pagerState,
                        beyondViewportPageCount = 1,
                        modifier = Modifier
                            .fillMaxSize()
                            .zoneClickable {
                                when (layoutDirection) {
                                    LayoutDirection.Ltr -> when (it) {
                                        Zone.LEFT -> scrollToPrevPage()
                                        Zone.MIDDLE -> menuVisible = !menuVisible
                                        Zone.RIGHT -> scrollToNextPage()
                                    }

                                    LayoutDirection.Rtl -> when (it) {
                                        Zone.LEFT -> scrollToNextPage()
                                        Zone.MIDDLE -> menuVisible = !menuVisible
                                        Zone.RIGHT -> scrollToPrevPage()
                                    }
                                }
                            }
                    ) { i ->
                       readerChapter.pages?.getOrNull(i)?.let {
                            ReaderPageImageItem(
                                page = it,
                                readerChapter = readerChapter,
                                onClick = {
                                    val pageWidthPx = with(density) { screenWidthDp.dp.toPx() }
                                    val zoneSplit = pageWidthPx / 3

                                    val zone = when(it.x){
                                        in 0f..zoneSplit -> Zone.LEFT
                                        in zoneSplit..(pageWidthPx - zoneSplit) -> Zone.MIDDLE
                                        else -> Zone.RIGHT
                                    }

                                    when (layoutDirection) {
                                        LayoutDirection.Ltr -> when (zone) {
                                            Zone.LEFT -> scrollToPrevPage()
                                            Zone.MIDDLE -> menuVisible = !menuVisible
                                            Zone.RIGHT -> scrollToNextPage()
                                        }

                                        LayoutDirection.Rtl -> when (zone) {
                                            Zone.LEFT -> scrollToNextPage()
                                            Zone.MIDDLE -> menuVisible = !menuVisible
                                            Zone.RIGHT -> scrollToPrevPage()
                                        }
                                    }
                                }
                            )
                        }
                            ?: CenterBox(modifier = Modifier.fillMaxSize()) {
                                CircularProgressIndicator()
                            }
                    }
                }
            }
            if (showPageNumber) {
                PageIndicatorText(
                    currentPage = pagerState.currentPage + 1,
                    totalPages = pagerState.pageCount,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.systemBars)
                )
            }
        }
    }
}

@Composable
fun ReaderPageImageItem(
    page: ReaderPage,
    readerChapter: ReaderChapter,
    onClick: (offset: Offset) -> Unit
) {
    val status by page.statusFlow.collectAsState()

    LaunchedEffect(page, readerChapter.pageLoader) {
        withContext(Dispatchers.IO) {
            readerChapter.pageLoader?.loadPage(page)
        }
    }

    when (status) {
        Page.State.READY -> {

            var hasError by remember { mutableStateOf(false) }

            val imageData by remember(page.stream) {
                derivedStateOf {
                    page.stream?.let { ByteBuffer.wrap(it().readBytes()) }
                }
            }

            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                ZoomableAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageData)
                        .listener(
                            onSuccess = { _, _ -> hasError = false },
                            onStart = { hasError = false },
                            onCancel = { hasError = false    },
                            onError = { _, _ -> hasError = true }
                        )
                        .build(),
                    modifier = Modifier
                        .fillMaxSize(),
                    contentDescription = null,
                    onClick = { offset ->
                        onClick(offset)
                    }
                )
                if (hasError) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "A problem occurred while loading the page",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "error",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        Page.State.QUEUE -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Queued")
                CircularProgressIndicator()
            }
        }

        Page.State.LOAD_PAGE, Page.State.DOWNLOAD_IMAGE -> {
            val surfaceColor = MaterialTheme.colorScheme.surface
            val primaryColor = MaterialTheme.colorScheme.primary
            val space = LocalSpacing.current

            val progress by page.progressFlow.collectAsStateWithLifecycle(0)

            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally

            ) {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        page.chapter.chapter.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(space.xs))
                    Text(
                        remember(page.chapter.chapter.title) { "Vol. ${page.chapter.chapter.volume} Ch. ${page.chapter.chapter.chapter} - ${page.chapter.chapter.title}" },
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                val widthPct by animateFloatAsState(
                    targetValue = progress.toFloat(),
                    label = "progress-anim"
                )

                Canvas(
                    modifier = Modifier
                        .padding(space.med)
                        .height(22.dp)
                        .fillMaxWidth()
                        .clip(CircleShape)
                ) {
                    drawRoundRect(
                        color = surfaceColor,
                        size = Size(this.size.width, this.size.height)
                    )
                    drawRoundRect(
                        color = primaryColor,
                        size = Size(
                            this.size.width * (widthPct / 100),
                            this.size.height
                        )
                    )
                }
            }
        }

        Page.State.ERROR -> {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "A problem occurred while loading the page",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "error",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun PageIndicatorText(
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

private enum class Zone {
    LEFT, MIDDLE, RIGHT
}

private enum class VerticalZone {
    Top, MIDDLE, Bottom
}

private fun Modifier.verticalZoneClickable(
    onClick: (VerticalZone) -> Unit
) = pointerInput(Unit) {

    val zoneSplit = this.size.height / 3

    detectTapGestures {
        val zone = when(it.y.roundToInt()){
            in 0..zoneSplit -> VerticalZone.Top
            in zoneSplit..(size.height - zoneSplit) -> VerticalZone.MIDDLE
            else -> VerticalZone.Bottom
        }
        onClick(zone)
    }
}

private fun Modifier.zoneClickable(
    onClick: (Zone) -> Unit
) = pointerInput(Unit) {

    val zoneSplit = this.size.width / 3

    detectTapGestures {
        val zone = when(it.x.roundToInt()){
            in 0..zoneSplit -> Zone.LEFT
            in zoneSplit..(size.width - zoneSplit) -> Zone.MIDDLE
            else -> Zone.RIGHT
        }
        onClick(zone)
    }
}
