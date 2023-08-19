@file:OptIn(ExperimentalFoundationApi::class)

package io.silv.amadeus.ui.screens.manga_reader

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.silv.amadeus.LocalBottomBarVisibility
import io.silv.amadeus.data.ReaderSettings
import io.silv.amadeus.types.SavableChapter
import io.silv.amadeus.ui.composables.AnimatedBoxShimmer
import io.silv.amadeus.ui.screens.manga_reader.composables.ReaderMenuOverlay
import io.silv.amadeus.ui.shared.CenterBox
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.core.lerp
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import kotlin.math.absoluteValue


class MangaReaderScreen(
    private val mangaId: String,
    private val initialChapterId: String,
): Screen {


    @Composable
    override fun Content() {

        val sm = getScreenModel<MangaReaderSM>() {
            parametersOf(mangaId, initialChapterId)
        }

        val state by sm.mangaReaderState.collectAsStateWithLifecycle()
        val readerSettings by sm.readerSettings.collectAsState()

        var bottomBarVisibility by LocalBottomBarVisibility.current

        DisposableEffect(Unit) {
            bottomBarVisibility = false
            onDispose {
                bottomBarVisibility = true
            }
        }

        MangaReaderContent(
            state = state,
            readerSettings = readerSettings,
            onReaderSettingsChange = {
                sm.updateReaderSettings(it)
            },
            onChapterBookmarked = {
                sm.bookmarkChapter(it)
            },
            goToNextChapter = {
                sm.goToNextChapter(it)
            },
            goToPrevChapter = {
                sm.goToPrevChapter(it)
            },
            updatePage = { page, last ->
                sm.updateChapterPage(page, last)
            }
        )
    }
}

@Composable
fun MangaReaderContent(
    state: MangaReaderState,
    readerSettings: ReaderSettings,
    onReaderSettingsChange: (ReaderSettings) -> Unit,
    onChapterBookmarked: (id: String) -> Unit,
    goToNextChapter: (from: SavableChapter) -> Unit,
    goToPrevChapter: (from: SavableChapter) -> Unit,
    updatePage: (page: Int, last: Int) -> Unit
) {
    val navigator = LocalNavigator.current

    val scope = rememberCoroutineScope()
    val horizontalReaderState = rememberPagerState()
    val verticalReaderState = rememberLazyListState()

    when (state) {
        is MangaReaderState.Failure -> {
            CenterBox(Modifier.fillMaxSize()) {
                Text(state.message ?: "failed to load")
            }
        }
        MangaReaderState.Loading -> {
            CenterBox(Modifier.fillMaxSize()) {
                AnimatedBoxShimmer(Modifier.fillMaxSize(0.8f))
            }
        }
        is MangaReaderState.Success -> {
            val firstVisibleInVertical by remember {
                derivedStateOf { verticalReaderState.firstVisibleItemIndex }
            }

            LaunchedEffect(horizontalReaderState) {
                snapshotFlow { horizontalReaderState.currentPage }
                    .collect { updatePage(it + 1, state.pages.size) }
            }

            LaunchedEffect(verticalReaderState) {
                snapshotFlow { verticalReaderState.firstVisibleItemIndex }
                    .collect { updatePage(it + 1, state.pages.size) }
            }

            ReaderMenuOverlay(
                modifier = Modifier.fillMaxSize(),
                handleBackGesture = {
                    scope.launch {
                        when (it){
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
                },
                handleForwardGesture = {
                    scope.launch {
                        when (it){
                            Orientation.Vertical -> {
                                (firstVisibleInVertical + verticalReaderState.layoutInfo.visibleItemsInfo.size)
                                    .takeIf { it >= 0 && it <= state.pages.lastIndex}
                                    ?.let { verticalReaderState.animateScrollToItem(it) }
                            }
                            Orientation.Horizontal -> {
                                horizontalReaderState.animateScrollToPage(
                                    horizontalReaderState.currentPage + 1
                                )
                            }
                        }
                    }
                },
                onNavigationIconClick = {
                    navigator?.pop()
                },
                chapter = state.chapter,
                chapters = state.chapters,
                onPageChange = {
                    scope.launch {
                        when (readerSettings.orientation){
                            Orientation.Vertical -> verticalReaderState.animateScrollToItem(it)
                            Orientation.Horizontal -> horizontalReaderState.animateScrollToPage(it)
                        }
                    }
                },
                currentPage = when(readerSettings.orientation) {
                    Orientation.Vertical -> firstVisibleInVertical
                    Orientation.Horizontal -> horizontalReaderState.currentPage
                },
                mangaTitle = state.manga.titleEnglish,
                onPrevClick = {
                    goToPrevChapter(state.chapter)
                    scope.launch {
                        horizontalReaderState.scrollToPage(0)
                        verticalReaderState.scrollToItem(0)
                    }
                },
                onNextClick = {
                    goToNextChapter(state.chapter)
                    scope.launch {
                        horizontalReaderState.scrollToPage(0)
                        verticalReaderState.scrollToItem(0)
                    }
                },
                readerSettings = readerSettings,
                onSettingsChanged = onReaderSettingsChange,
                onChapterBookmarked = onChapterBookmarked,
                lastPage = state.pages.size,
            ) {
                MangaReader(
                    viewing = state.chapter,
                    images = state.pages,
                    prev = remember(state.chapters, state.chapter) {
                        val idx = state.chapters.map { it.id }.also { Log.d("CHAPTERS", it.toString()) }.indexOf(state.chapter.id)
                        state.chapters.getOrNull(idx - 1)
                    },
                    next = remember(state.chapters, state.chapter) {
                        val idx = state.chapters.map { it.id }.indexOf(state.chapter.id)
                        state.chapters.getOrNull(idx + 1)
                    },
                    settings = readerSettings,
                    verticalReaderState = verticalReaderState,
                    horizontalReaderState = horizontalReaderState
                )
            }
        }
    }
}



@Composable
fun MangaReader(
    verticalReaderState: LazyListState,
    horizontalReaderState: PagerState,
    viewing: SavableChapter,
    images: List<String>,
    prev: SavableChapter?,
    next: SavableChapter?,
    settings: ReaderSettings
) {
    val space = LocalSpacing.current
    when (settings.orientation) {
        Orientation.Vertical -> {
            VerticalReader(
                modifier = Modifier.fillMaxSize(),
                state = verticalReaderState,
                viewing = viewing,
                images = images,
                prev = prev,
                next = next,
            )
        }
        Orientation.Horizontal -> {
            Column(Modifier.fillMaxSize()) {
                HorizontalReader(
                    modifier = Modifier.weight(1f),
                    viewing = viewing,
                    images = images,
                    prev = prev,
                    next = next,
                    pagerState = horizontalReaderState,
                    reverseLayout = when(settings.direction) {
                        LayoutDirection.Ltr -> false
                        LayoutDirection.Rtl -> true
                    }
                )
                Spacer(modifier = Modifier.height(space.med))
                AnimatedPageNumber(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    mangaPagerState = horizontalReaderState,
                    pageCount = images.size,
                    hasPrev = prev != null
                )
                Spacer(modifier = Modifier.height(space.med))
            }
        }
    }
}

@Composable
fun VerticalReader(
    modifier: Modifier = Modifier,
    state: LazyListState,
    viewing: SavableChapter,
    images: List<String>,
    prev: SavableChapter?,
    next: SavableChapter?,
) {
    LazyColumn(modifier, state = state) {
        prev?.let {
            item {
                CenterBox(Modifier.fillMaxSize()) {
                    Text("${it.title}  ch ${it.chapter}")
                }
            }
        }
        items(images, key = { it }) {url ->
            MangaImage(
                modifier = Modifier.fillMaxSize(),
                url = url
            )
        }
        next?.let {
            item {
                CenterBox(Modifier.fillMaxSize()) {
                    Text("${it.title}  ch ${it.chapter}")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HorizontalReader(
    modifier: Modifier = Modifier,
    viewing: SavableChapter,
    pagerState: PagerState,
    images: List<String>,
    prev: SavableChapter?,
    next: SavableChapter?,
    reverseLayout: Boolean,
) {
    val pageCount =  images.size + if (prev != null) 1 else 0 + if (next != null) 1 else 0
    HorizontalPager(
        modifier = modifier,
        pageCount = pageCount,
        state = pagerState,
        pageSize = PageSize.Fill,
        reverseLayout = reverseLayout
    ) {
        if (it == 0) {
            prev?.let {
                CenterBox(Modifier.fillMaxSize()) {
                    Text("${it.title}  ch ${it.chapter}")
                }
                return@HorizontalPager
            }
        } else if (it >= images.size) {
            next?.let {
                CenterBox(Modifier.fillMaxSize()) {
                    Text("${it.title}  ch ${it.chapter}")
                }
                return@HorizontalPager
            }
        }
        MangaImage(
            modifier = Modifier.fillMaxSize(),
            url = images.getOrElse(it - if (prev != null) 1 else 0) { "" }
        )
    }
}

@Composable
fun MangaImage(
    modifier: Modifier,
    url: String
) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .build(),
        modifier= modifier,
        contentScale = ContentScale.Fit,
        contentDescription = null
    )
}




@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimatedPageNumber(
    modifier: Modifier = Modifier,
    state: PagerState = rememberPagerState(),
    mangaPagerState: PagerState,
    hasPrev: Boolean,
    pageCount: Int,
) {

    LaunchedEffect(Unit) {
        snapshotFlow { mangaPagerState.currentPage }.collect {
            state.animateScrollToPage(it)
        }
    }

    var offset by remember {
        mutableStateOf(0.dp)
    }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val density = LocalDensity.current

    HorizontalPager(
        pageCount = pageCount,
        state = state,
        userScrollEnabled = false,
        pageSize = PageSize.Fixed(50.dp),
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = (screenWidth / 2f) - offset
        )
    ) { page ->
        CenterBox(
            Modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    offset = with(density) { (it.size.width / 2).toDp() }
                }
                .graphicsLayer {
                    val pageOffset = ((mangaPagerState.currentPage - page) + mangaPagerState
                        .currentPageOffsetFraction
                            ).absoluteValue
                    val interpolation = lerp(
                        start = 0.5f,
                        stop = 1f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    )
                    scaleX = interpolation
                    scaleY = interpolation
                    alpha = interpolation
                }
        ) {
            if (hasPrev && pageCount == 0) {
                Text(text = " ")
            } else {
                Text(
                    text = (page + if(!hasPrev) 1 else 0).toString(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}