@file:OptIn(ExperimentalFoundationApi::class)

package io.silv.amadeus.ui.screens.manga_reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import io.silv.amadeus.LocalBottomBarVisibility
import io.silv.amadeus.ui.composables.AnimatedBoxShimmer
import io.silv.amadeus.ui.screens.manga_reader.composables.ReaderMenuOverlay
import io.silv.amadeus.ui.screens.manga_reader.composables.rememberGestureHandler
import io.silv.amadeus.ui.shared.CenterBox
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.common.model.ReaderDirection
import io.silv.common.model.ReaderOrientation
import io.silv.datastore.model.ReaderSettings
import io.silv.model.SavableChapter
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
            },
            goToChapter = { id ->
                sm.goToChapter(id)
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UpdateReaderState(
    readerChapters: ReaderChapters,
    horizontalReaderState: PagerState,
    verticalReaderState: LazyListState,
    updatePage: (page: Int, last: Int) -> Unit,
) {

    val update by rememberUpdatedState(updatePage)

    LaunchedEffect(horizontalReaderState) {
        snapshotFlow { horizontalReaderState.currentPage }
            .collect { update(it + 1, readerChapters.chapterImages.size) }
    }

    LaunchedEffect(verticalReaderState) {
        snapshotFlow { verticalReaderState.firstVisibleItemIndex }
            .collect { update(it + 1, readerChapters.chapterImages.size) }
    }

    LaunchedEffect(readerChapters.current.id) {
        launch { verticalReaderState.scrollToItem(0) }
        launch { horizontalReaderState.scrollToPage(0) }
    }
}



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MangaReaderContent(
    state: MangaReaderState,
    readerSettings: ReaderSettings,
    onReaderSettingsChange: (ReaderSettings) -> Unit,
    onChapterBookmarked: (id: String) -> Unit,
    goToNextChapter: (from: SavableChapter) -> Unit,
    goToPrevChapter: (from: SavableChapter) -> Unit,
    updatePage: (page: Int, last: Int) -> Unit,
    goToChapter: (id: String) -> Unit,
) {
    val navigator = LocalNavigator.current

    val scope = rememberCoroutineScope()

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
            val horizontalReaderState = rememberPagerState(
                initialPage = state.readerChapters.current.lastReadPage,
                pageCount = {
                    state.readerChapters.chapterImages.size +
                            listOf(state.readerChapters.hasPrev, state.readerChapters.hasNext)
                                .count { it }
                }
            )
            val verticalReaderState = rememberLazyListState(
                initialFirstVisibleItemIndex = state.readerChapters.current.lastReadPage
            )
            val gestureHandler = rememberGestureHandler(
                imageListSize = state.readerChapters.chapterImages.size,
                verticalReaderState = verticalReaderState,
                horizontalReaderState = horizontalReaderState
            )

            UpdateReaderState(
                readerChapters = state.readerChapters,
                horizontalReaderState = horizontalReaderState,
                verticalReaderState = verticalReaderState,
                updatePage = updatePage
            )

            ReaderMenuOverlay(
                modifier = Modifier.fillMaxSize(),
                handleBackGesture = { gestureHandler.handleBackGesture(it) },
                handleForwardGesture = { gestureHandler.handleForwardGesture(it) },
                goToChapter = goToChapter,
                onNavigationIconClick = {
                    navigator?.pop()
                },
                chapter = state.readerChapters.current,
                chapters = state.chapters,
                onPageChange = {
                    scope.launch {
                        when (readerSettings.orientation){
                            ReaderOrientation.Vertical -> verticalReaderState.animateScrollToItem(it)
                            ReaderOrientation.Horizontal -> horizontalReaderState.animateScrollToPage(it)
                        }
                    }
                },
                currentPage = when(readerSettings.orientation) {
                    ReaderOrientation.Vertical-> gestureHandler.firstVisibleInVertical
                    ReaderOrientation.Horizontal -> horizontalReaderState.currentPage
                },
                mangaTitle = state.manga.titleEnglish,
                onPrevClick = {
                    goToPrevChapter(state.readerChapters.current)
                    scope.launch {
                        horizontalReaderState.scrollToPage(0)
                        verticalReaderState.scrollToItem(0)
                    }
                },
                onNextClick = {
                    goToNextChapter(state.readerChapters.current)
                    scope.launch {
                        horizontalReaderState.scrollToPage(0)
                        verticalReaderState.scrollToItem(0)
                    }
                },
                readerSettings = readerSettings,
                onSettingsChanged = onReaderSettingsChange,
                onChapterBookmarked = onChapterBookmarked,
                lastPage = state.readerChapters.chapterImages.size,
            ) {
                MangaReader(
                    readerChapters = state.readerChapters,
                    settings = readerSettings,
                    verticalReaderState = verticalReaderState,
                    horizontalReaderState = horizontalReaderState,
                    readChapter = { id -> goToChapter(id) }
                )
            }
        }
    }
}



@Composable
fun MangaReader(
    verticalReaderState: LazyListState,
    horizontalReaderState: PagerState,
    readerChapters: ReaderChapters,
    settings: io.silv.datastore.model.ReaderSettings,
    readChapter: (id: String) -> Unit,
) {
    val space = LocalSpacing.current
    when (settings.orientation) {
        ReaderOrientation.Vertical -> {
            VerticalReader(
                modifier = Modifier.fillMaxSize(),
                state = verticalReaderState,
                readerChapters = readerChapters,
                readChapter = readChapter
            )
        }
        ReaderOrientation.Horizontal -> {
            Column(Modifier.fillMaxSize()) {
                HorizontalReader(
                    modifier = Modifier.weight(1f),
                    readerChapters = readerChapters,
                    pagerState = horizontalReaderState,
                    reverseLayout = when(settings.direction) {
                        ReaderDirection.Ltr -> false
                        ReaderDirection.Rtl -> true
                    },
                    readChapter = readChapter
                )
                Spacer(modifier = Modifier.height(space.med))
                AnimatedPageNumber(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    mangaPagerState = horizontalReaderState,
                    pageCount = readerChapters.chapterImages.size,
                    hasPrev = readerChapters.hasPrev,
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
    readerChapters: ReaderChapters,
    readChapter: (id: String) -> Unit
) {
    LazyColumn(modifier, state = state) {
        if (readerChapters.prev != null) {
            item {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Previous chapter")
                    Text("${readerChapters.prev.title}  ch ${readerChapters.prev.chapter}")
                    Button(onClick = { readChapter(readerChapters.prev.id)  }) {
                        Text("Go to previous")
                    }
                }
            }
        }
        items(readerChapters.chapterImages, key = { it }) {url ->
            MangaImage(
                modifier = Modifier.fillMaxSize(),
                url = url
            )
        }
        if(readerChapters.next != null) {
            item {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Next chapter")
                    Text("${readerChapters.next.title}  ch ${readerChapters.next.chapter}")
                    Button(onClick = { readChapter(readerChapters.next.id)  }) {
                        Text("Go to next")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HorizontalReader(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    readerChapters: ReaderChapters,
    reverseLayout: Boolean,
    readChapter: (id: String) -> Unit
) {
    HorizontalPager(
        modifier = modifier,
        state = pagerState,
        pageSize = PageSize.Fill,
        reverseLayout = reverseLayout
    ) { page ->
        if (page == 0 && readerChapters.hasPrev && readerChapters.prev != null) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Previous chapter")
                Text("${readerChapters.prev.title}  ch ${readerChapters.prev.chapter}")
                Button(onClick = { readChapter(readerChapters.prev.id)  }) {
                    Text("Go to prev")
                }
            }
        } else if (page > readerChapters.chapterImages.lastIndex && readerChapters.next != null) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Next chapter")
                Text("${readerChapters.next.title}  ch ${readerChapters.next.chapter}")
                Button(onClick = { readChapter(readerChapters.next.id)  }) {
                    Text("Go to next")
                }
            }
        } else {
            MangaImage(
                modifier = Modifier.fillMaxSize(),
                url = readerChapters.chapterImages[page - if (readerChapters.hasPrev) 1 else 0]
            )
        }
    }
}

@Composable
fun MangaImage(
    modifier: Modifier,
    url: String,
) {

    val context = LocalContext.current

    var imageState by remember {
        mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
    }

    if (imageState is AsyncImagePainter.State.Error) {
        CenterBox(modifier.padding(32.dp)) {
            Text(
                text = "error loading the image,\n" +
                        "format provided by the source may not be supported",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                ),
            )
        }
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .build(),
        onState = {
            imageState = it
        },
        modifier= modifier,
        contentScale = ContentScale.Fit,
        contentDescription = null
    )
}




@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimatedPageNumber(
    pageCount: Int,
    modifier: Modifier = Modifier,
    state: PagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f,
        pageCount = { pageCount }
    ),
    mangaPagerState: PagerState,
    hasPrev: Boolean,
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
                    val interpolation = io.silv.common.lerp(
                        start = 0.5f,
                        stop = 1f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    )
                    scaleX = interpolation
                    scaleY = interpolation
                    alpha = interpolation
                }
        ) {
            if (hasPrev && page == 0) {
                Text(text = " ")
            } else {
                Text(
                    text = (page + if (!hasPrev) 1 else 0).toString(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}