package io.silv.manga.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.skydoves.sandwich.message
import com.skydoves.sandwich.onFailure
import com.skydoves.sandwich.onSuccess
import io.silv.common.DependencyAccessor
import io.silv.common.model.Download
import io.silv.common.model.Filters
import io.silv.data.download.QItem
import io.silv.di.dataDeps
import io.silv.manga.composables.MangaDescription
import io.silv.manga.composables.MangaImageWithTitle
import io.silv.navigation.SharedScreen
import io.silv.navigation.push
import io.silv.ui.CenterBox
import io.silv.ui.LocalAppState
import io.silv.ui.collectEvents
import io.silv.ui.design.chapterListItems
import io.silv.ui.isScrollingUp
import io.silv.ui.layout.PullRefresh
import io.silv.ui.layout.ScrollbarLazyColumn
import io.silv.ui.openOnWeb
import io.silv.ui.theme.LocalSpacing

import kotlinx.coroutines.launch

class MangaViewScreen(
    private val mangaId: String,
) : Screen {

    override val key: ScreenKey = "MangaViewScreen_$mangaId"

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { MangaViewScreenModel(mangaId = mangaId) }

        val state by screenModel.state.collectAsStateWithLifecycle()
        val appState = LocalAppState.current

        screenModel.collectEvents { event ->
            when (event) {
                is MangaViewEvent.FailedToLoadChapterList -> appState.showSnackBar(event.message)
                is MangaViewEvent.FailedToLoadVolumeArt -> appState.showSnackBar(event.message)
                is MangaViewEvent.BookmarkStatusChanged -> {
                    appState.showSnackBar(
                        message =
                            if (event.bookmarked) {
                                "Bookmarked"
                            } else {
                                "Removed Bookmark"
                            },
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short,
                        onActionPerformed = {
                            screenModel.changeChapterBookmarked(
                                event.id
                            )
                        }
                    )
                }

                is MangaViewEvent.ReadStatusChanged -> {
                    appState.showSnackBar(
                        message =
                            if (event.read) {
                                "Marked as read"
                            } else {
                                "Marked as unread"
                            },
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short,
                        onActionPerformed = {
                            screenModel.changeChapterReadStatus(
                                event.id
                            )
                        }
                    )
                }
            }
        }

        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        MangaViewScreenContent(
            state = state,
            viewOnWeb = { url ->
                context.openOnWeb(url, "View manga using.")
                    .onFailure {
                        scope.launch {
                            appState.showSnackBar("Couldn't open url.")
                        }
                    }
            },
            filterActions =
                FilterActions(
                    downloaded = screenModel::filterDownloaded,
                    uploadDate = screenModel::filterByUploadDate,
                    chapterNumber = screenModel::filterByChapterNumber,
                    source = screenModel::filterBySource,
                    bookmarked = screenModel::filterBookmarked,
                    unread = screenModel::filterUnread,
                    setAsDefault = {},
                ),
            chapterActions =
                ChapterActions(
                    bookmark = screenModel::changeChapterBookmarked,
                    read = screenModel::changeChapterReadStatus,
                    download = screenModel::downloadChapterImages,
                    delete = screenModel::deleteChapterImages,
                    refresh = screenModel::refreshChapterList
                ),
            mangaActions =
                MangaActions(
                    addToLibrary = screenModel::toggleLibraryManga,
                    changeStatus = screenModel::updateMangaReadingStatus
                ),
            downloadActions = DownloadActions(
                pause = screenModel::pauseDownload,
                cancel = screenModel::cancelDownload
            )
        )
    }
}

@Composable
fun MangaViewScreenContent(
    state: MangaViewState,
    viewOnWeb: (url: String) -> Unit,
    filterActions: FilterActions,
    chapterActions: ChapterActions,
    mangaActions: MangaActions,
    downloadActions: DownloadActions,
) {
    val navigator = LocalNavigator.currentOrThrow
    when (val mangaState = state.mangaState) {
        is MangaState.Error ->
            CenterBox(Modifier.fillMaxSize()) {
                Column {
                    Text(mangaState.message)
                    Button(onClick = { navigator.pop() }) {
                        Text(text = "Go back")
                    }
                }
            }

        MangaState.Loading ->
            CenterBox(Modifier.fillMaxSize()) {
                CircularProgressIndicator()
            }

        is MangaState.Success ->
            MangaViewSuccessScreen(
                state = mangaState,
                viewOnWeb = viewOnWeb,
                filterActions = filterActions,
                chapterActions = chapterActions,
                mangaActions = mangaActions,
                downloadActions = downloadActions,
                downloads = state.downloads,
                filters = state.filters,
                statsUiState = state.statsUiState
            )
    }
}

private sealed interface MangaViewBottomSheet {
    data object Filter: MangaViewBottomSheet
    data object VolumeArt: MangaViewBottomSheet
}

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, DependencyAccessor::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun MangaViewSuccessScreen(
    downloads: List<QItem<Download>>,
    state: MangaState.Success,
    filters: Filters,
    statsUiState: StatsUiState,
    viewOnWeb: (url: String) -> Unit,
    filterActions: FilterActions,
    chapterActions: ChapterActions,
    mangaActions: MangaActions,
    downloadActions: DownloadActions
) {
    val navigator = LocalNavigator.currentOrThrow
    val topBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
        state = topBarState
    )
    val listState = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentBottomSheet by rememberSaveable { mutableStateOf<MangaViewBottomSheet?>(null) }

    var showSourceTitle by rememberSaveable { mutableStateOf(true) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {

            val collapsed by remember {
                derivedStateOf {
                    topBarState.overlappedFraction == 1f
                }
            }

            TopAppBar(
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                            .copy(alpha = scrollBehavior.state.overlappedFraction.coerceIn(0f, 1f)),
                    ),
                title = {
                    AnimatedVisibility(
                        visible = collapsed,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        Text(
                            text = state.manga.titleEnglish,
                            maxLines = 1,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Start,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navigator.pop()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    val primary = MaterialTheme.colorScheme.primary
                    val background = MaterialTheme.colorScheme.onBackground

                    val (icon, tint) = remember(state.manga, primary, background) {
                        if (state.manga.inLibrary)
                            Icons.Filled.Favorite to primary
                        else
                            Icons.Outlined.FavoriteBorder to background
                    }

                    AnimatedVisibility(visible = collapsed) {
                        IconButton(onClick = {
                            mangaActions.addToLibrary(state.manga.id)
                        }) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = tint
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            viewOnWeb("https://mangadex.org/title/${state.manga.id}")
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Web,
                            contentDescription = "View on web",
                        )
                    }
                    IconButton(
                        onClick = { currentBottomSheet = MangaViewBottomSheet.Filter },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FilterList,
                            contentDescription = "filter",
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.systemBars),
        floatingActionButton = {
            AnimatedVisibility(
                visible = state.chapters.fastAny { !it.read },
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                val space = LocalSpacing.current
                ExtendedFloatingActionButton(
                    text = {
                        val text =
                            remember(state.chapters) {
                                if (state.chapters.fastAny { it.started || it.read }) "Resume" else "Start"
                            }
                        Text(text = text)
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    icon = {
                        Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                    },
                    onClick = {
                        val lastUnread =
                            state.chapters
                                .sortedBy { chapter ->
                                    chapter.takeIf { it.validNumber }?.chapter ?: Double.MAX_VALUE
                                }
                                .fastFirstOrNull { !it.read }
                                ?: state.success?.chapters?.minByOrNull { it.chapter }
                                ?: return@ExtendedFloatingActionButton

                        navigator.push(SharedScreen.Reader(lastUnread.mangaId, lastUnread.id))
                    },
                    modifier = Modifier.padding(vertical = space.large),
                    expanded = listState.isScrollingUp(),
                )
            }
        },
    ) { paddingValues ->
        PullRefresh(
            refreshing = state.refreshingChapters,
            onRefresh = { chapterActions.refresh() }
        ) {
            ScrollbarLazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                        end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                    ),
            ) {
                item(key = "manga-poster") {
                    MangaImageWithTitle(
                        manga = state.manga,
                        modifier = Modifier
                            .fillMaxWidth(),
                        padding = paddingValues,
                        stats = statsUiState,
                        viewOnWeb = {
                            viewOnWeb("https://mangadex.org/title/${state.manga.id}")
                        },
                        addToLibrary = mangaActions.addToLibrary,
                        showChapterArt = {
                            currentBottomSheet = MangaViewBottomSheet.VolumeArt
                        },
                        changeStatus = { mangaActions.changeStatus(it) }
                    )
                }
                item("manga-info") {
                    Column {
                        MangaDescription(
                            manga = state.manga,
                            onTagSelected = { tag ->
                                state.manga.tagToId[tag]?.let { id ->
                                    navigator.push(SharedScreen.MangaFilter(tag, id))
                                }
                            },
                        )
                        Spacer(modifier = Modifier.height(22.dp))
                        val maxChapterNum =
                            remember(state.chapters) {
                                state.chapters.maxOfOrNull { it.chapter } ?: 0
                            }

                        val missingChapters =
                            remember(state.chapters) {
                                var prevChapter: Double? = null
                                state.chapters
                                    .distinctBy { it.chapter }
                                    .filter { it.validNumber }
                                    .sortedBy { it.chapter }
                                    .count { c ->
                                        (c.chapter - 1 != prevChapter && prevChapter != null)
                                            .also { prevChapter = c.chapter }
                                    }
                            }

                        Text(
                            text = "$maxChapterNum chapters - missing $missingChapters",
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
                chapterListItems(
                    items = state.filteredChapters,
                    downloads = downloads,
                    onDownloadClicked = chapterActions.download,
                    onDeleteClicked = chapterActions.delete,
                    onReadClicked = {
                        navigator.push(SharedScreen.Reader(state.manga.id, it))
                    },
                    onBookmark = chapterActions.bookmark,
                    onMarkAsRead = chapterActions.read,
                    showFullTitle = showSourceTitle,
                    pauseDownload = { downloadActions.pause(it) },
                    cancelDownload = { downloadActions.cancel(it) }
                )
                item(key = "padding-bottom") {
                    // apply both top and bottom padding to raise chapter list above the FAB
                    // top padding is ignored for manga cover art.
                    Spacer(
                        Modifier.height(
                            paddingValues.calculateBottomPadding() + paddingValues.calculateTopPadding(),
                        ),
                    )
                }
            }
        }
    }

    when (currentBottomSheet) {
        null -> Unit
        MangaViewBottomSheet.Filter ->
            FilterBottomSheet(
                visible = true,
                filters = filters,
                sheetState = sheetState,
                onDismiss = { currentBottomSheet = null },
                onSetAsDefaultClick = filterActions.setAsDefault,
                filterDownloaded = filterActions.downloaded,
                filterUnread = filterActions.unread,
                filterBookmarked = filterActions.bookmarked,
                filterBySource = filterActions.source,
                filterByChapterNumber = filterActions.chapterNumber,
                filterByUploadDate = filterActions.uploadDate,
                showingSourceTitle = showSourceTitle,
                hideSourceTitle = { showSourceTitle = false },
                showSourceTitle = { showSourceTitle = true },
            )

        MangaViewBottomSheet.VolumeArt ->
            ModalBottomSheet(
                contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
                onDismissRequest = { currentBottomSheet = null },
                sheetState = sheetState,
                modifier = Modifier
                    .fillMaxHeight(0.6f)
                    .fillMaxWidth()
            ) {

                var coverImages by remember {
                    mutableStateOf<List<String>>(emptyList())
                }
                var error by remember { mutableStateOf<String?>(null) }
                var loading by remember { mutableStateOf(false) }
                var loadingSem by remember { mutableIntStateOf(0) }
                val getMangaCoverArtById = remember { dataDeps.getMangaCoverArtById }

                LaunchedEffect(loadingSem) {
                    loading = true
                    getMangaCoverArtById.await(state.manga.id)
                        .onSuccess {
                            error = null
                            coverImages = data
                        }
                        .onFailure {
                            error = message()
                        }
                    loading = false
                }

                if (coverImages.isEmpty() || loading) {
                    CenterBox(
                        Modifier
                            .height(400.dp)
                            .fillMaxWidth()
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (error != null) {
                    CenterBox(Modifier.height(400.dp)) {
                        TextButton(
                            onClick = { loadingSem++ }
                        ) {
                            Text(
                                buildAnnotatedString {
                                    append("Error while loading manga click to retry")
                                }
                            )
                        }
                    }
                } else {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val pagerState = rememberPagerState { coverImages.size }
                        HorizontalPager(
                            state = pagerState,
                            beyondViewportPageCount = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            val space = LocalSpacing.current

                            AsyncImage(
                                model = coverImages.getOrNull(it),
                                placeholder = ColorPainter(Color(0x1F888888)),
                                contentDescription = null,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .padding(space.med),
                                contentScale = ContentScale.Fit,
                            )
                        }
                        PageIndicatorText(
                            currentPage = pagerState.currentPage + 1,
                            totalPages = pagerState.pageCount,
                            modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars)
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

@Composable
fun MangaStats(
    state: StatsUiState,
    modifier: Modifier = Modifier,
    ratingItem: @Composable RowScope.(rating: Double) -> Unit = { rating ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val space = LocalSpacing.current
            val text =
                remember(rating) {
                    "%.2f / 10".format(rating)
                }
            Icon(
                imageVector = Icons.Filled.StarRate,
                contentDescription = "rating",
                Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(space.med))
            Text(text, style = MaterialTheme.typography.titleSmall)
        }
    },
    commentsItem: @Composable RowScope.(comments: Int) -> Unit = { comments ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val space = LocalSpacing.current
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Comment,
                contentDescription = "comments",
                Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(space.med))
            Text(remember(comments) { "$comments" }, style = MaterialTheme.typography.titleSmall)
        }
    },
    followsItem: @Composable RowScope.(follows: Int) -> Unit = { follows ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val space = LocalSpacing.current
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "follows",
                Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(space.med))
            Text(remember(follows) { "$follows" }, style = MaterialTheme.typography.titleSmall)
        }
    },
) {
    val space = LocalSpacing.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space.med),
    ) {
        when (state) {
            is StatsUiState.Error -> {}
            StatsUiState.Loading -> {}
            is StatsUiState.Success -> {
                if (state.stats.validRating) {
                    ratingItem(state.stats.rating)
                    Spacer(modifier = Modifier.width(space.small))
                }
                if (state.stats.validComments) {
                    commentsItem(state.stats.comments)
                    Spacer(modifier = Modifier.width(space.small))
                }
                if (state.stats.validFollows) {
                    followsItem(state.stats.follows)
                }
            }
        }
    }
}
