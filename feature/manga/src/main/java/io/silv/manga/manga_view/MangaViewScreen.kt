package io.silv.manga.manga_view

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.silv.datastore.model.Filters
import io.silv.manga.composables.MainPoster
import io.silv.manga.composables.MangaContent
import io.silv.manga.composables.WebViewOverlay
import io.silv.manga.composables.chapterListItems
import io.silv.manga.composables.volumePosterItems
import io.silv.navigation.SharedScreen
import io.silv.navigation.push
import io.silv.ui.CenterBox
import io.silv.ui.collectEvents
import io.silv.ui.isScrollingUp
import io.silv.ui.layout.ScrollbarLazyColumn
import io.silv.ui.theme.LocalSpacing
import kotlinx.collections.immutable.persistentListOf
import org.koin.core.parameter.parametersOf


class MangaViewScreen(
    private val mangaId: String,
): Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {

        val screenModel = getScreenModel<MangaViewScreenModel> { parametersOf(mangaId) }

        val state by screenModel.state.collectAsStateWithLifecycle()

        val snackbarHostState = remember { SnackbarHostState() }

        screenModel.collectEvents { event ->
            suspend fun showSnackBar(message: String) {
                snackbarHostState.showSnackbar(
                    message = message,
                    withDismissAction = true,
                    duration = SnackbarDuration.Short
                )
            }
            when (event) {
                is MangaViewEvent.FailedToLoadChapterList -> showSnackBar(event.message)
                is MangaViewEvent.FailedToLoadVolumeArt -> showSnackBar(event.message)
                is MangaViewEvent.BookmarkStatusChanged -> {
                    val result = snackbarHostState.showSnackbar(
                        message = if (event.bookmarked)
                            "Bookmarked"
                        else
                            "Removed Bookmark",
                        withDismissAction = true,
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    when (result) {
                        SnackbarResult.Dismissed -> Unit
                        SnackbarResult.ActionPerformed -> screenModel.changeChapterBookmarked(event.id)
                    }
                }

                is MangaViewEvent.ReadStatusChanged -> {
                    val result = snackbarHostState.showSnackbar(
                        message = if (event.read)
                            "Marked as read"
                        else
                            "Marked as unread",
                        withDismissAction = true,
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    when (result) {
                        SnackbarResult.Dismissed -> Unit
                        SnackbarResult.ActionPerformed -> screenModel.changeChapterReadStatus(event.id)
                    }
                }
            }
        }

        var webUrl by rememberSaveable { mutableStateOf<String?>(null) }

        BackHandler(
            enabled = webUrl != null
        ) {
            webUrl = null
        }

        if (webUrl != null) {
            webUrl?.let {
                WebViewOverlay(
                    base = it
                )
            }
            return
        }


        MangaViewScreenContent(
            state = state,
            snackbarHostState = snackbarHostState,
            viewOnWeb = { url -> webUrl = url },
            filterActions = FilterActions(
                downloaded = screenModel::filterDownloaded,
                uploadDate = screenModel::filterByUploadDate,
                chapterNumber = screenModel::filterByChapterNumber,
                source = screenModel::filterBySource,
                bookmarked = screenModel::filterBookmarked,
                unread = screenModel::filterUnread,
                setAsDefault = screenModel::setFilterAsDefault
            ),
            chapterActions = ChapterActions(
                bookmark = screenModel::changeChapterBookmarked,
                read = screenModel::changeChapterReadStatus,
                download = screenModel::downloadChapterImages,
                delete = screenModel::deleteChapterImages
            ),
            mangaActions = MangaActions(
                addToLibrary = screenModel::addMangaToLibrary
            )
        )

    }
}


@Composable
fun MangaViewScreenContent(
    state: MangaViewState,
    snackbarHostState: SnackbarHostState,
    viewOnWeb: (url: String) -> Unit,
    filterActions: FilterActions,
    chapterActions: ChapterActions,
    mangaActions: MangaActions,
) {
    val navigator = LocalNavigator.currentOrThrow
    when (state) {
        is MangaViewState.Error -> CenterBox(Modifier.fillMaxSize()) {
            Column {
                Text(state.message)
                Button(onClick = { navigator.pop() }) {
                    Text(text = "Go back")
                }
            }
        }
        MangaViewState.Loading -> CenterBox(Modifier.fillMaxSize()) {
            CircularProgressIndicator()
        }
        is MangaViewState.Success -> MangaViewSuccessScreen(
            state = state,
            snackbarHostState = snackbarHostState,
            viewOnWeb = viewOnWeb,
            filterActions = filterActions,
            chapterActions = chapterActions,
            mangaActions = mangaActions
        )
    }
}

private const val FILTER_BOTTOM_SHEET = 1
private const val VOLUME_ART_BOTTOM_SHEET = 0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaViewSuccessScreen(
    state: MangaViewState.Success,
    snackbarHostState: SnackbarHostState,
    viewOnWeb: (url: String) -> Unit,
    filterActions: FilterActions,
    chapterActions: ChapterActions,
    mangaActions: MangaActions
) {
    val navigator = LocalNavigator.currentOrThrow
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(state = rememberTopAppBarState())
    val listState = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,)
    var currentBottomSheet by rememberSaveable { mutableStateOf<Int?>(null) }

    var showSourceTitle by rememberSaveable {
        mutableStateOf(true)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                title = {},
                navigationIcon = {
                    IconButton(onClick = {
                        navigator.pop()
                    }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = null)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                        val text = remember(state.chapters) {
                            if (state.chapters.fastAny { it.started || it.read }) "Resume" else "Start"
                        }
                        Text(text = text)
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    icon = {
                        Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                    },
                    onClick = {
                        val lastUnread = state.chapters
                            .sortedBy { chapter -> chapter.chapter.takeIf { it >= 0 } ?: Long.MAX_VALUE }
                            .fastFirstOrNull { !it.read }
                            ?: state.success?.chapters?.minByOrNull { it.chapter }
                            ?: return@ExtendedFloatingActionButton

                        navigator.push(SharedScreen.Reader(lastUnread.mangaId, lastUnread.id))
                    },
                    modifier = Modifier.padding(vertical = space.large),
                    expanded = listState.isScrollingUp(),
                )
            }
        }
    ) { paddingValues ->
        ScrollbarLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
            )
        ) {
            item(key = "manga-poster") {
              MainPoster(
                  manga = state.manga,
                  modifier = Modifier.fillMaxWidth(),
                  padding = paddingValues
              )
            }
            item("manga-info") {
                Column(
                    Modifier.padding(horizontal = 12.dp)
                ) {
                    MangaStats(
                        modifier = Modifier.fillMaxWidth(),
                        state = state.statsUiState
                    )
                    MangaContent(
                        manga = state.manga,
                        bookmarked = state.manga.bookmarked,
                        onBookmarkClicked = mangaActions.addToLibrary,
                        onTagSelected = { tag ->
                            state.manga.tagToId[tag]?.let { id ->
                                navigator.push(SharedScreen.MangaFilter(tag, id))
                            }
                        },
                        viewOnWebClicked = {
                            viewOnWeb("https://mangadex.org/title/${state.manga.id}")
                        },
                        showChapterArt = {
                            currentBottomSheet = VOLUME_ART_BOTTOM_SHEET
                        }
                    )
                    FilterIcon(
                        modifier = Modifier.align(Alignment.End),
                        onClick = {
                            currentBottomSheet = FILTER_BOTTOM_SHEET
                        }
                    )
                }
            }
            chapterListItems(
                mangaViewState = state,
                downloadingIds = persistentListOf(),
                onDownloadClicked = chapterActions.download,
                onDeleteClicked = chapterActions.delete,
                onReadClicked = {
                    navigator.push(SharedScreen.Reader(state.manga.id, it))
                },
                onBookmark = chapterActions.bookmark,
                onMarkAsRead = chapterActions.read,
                showFullTitle = showSourceTitle,
            )
            item(key = "padding-bottom") {
                // apply both top and bottom padding to raise chapter list above the FAB
                // top padding is ignored for manga cover art.
                Spacer(
                    Modifier.height(
                        paddingValues.calculateBottomPadding() + paddingValues.calculateTopPadding()
                    )
                )
            }
        }
    }


    when(currentBottomSheet) {
        FILTER_BOTTOM_SHEET -> FilterBottomSheet(
            visible = true,
            filters = state.filters,
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
            showSourceTitle = { showSourceTitle = true }
        )
        VOLUME_ART_BOTTOM_SHEET ->  ModalBottomSheet(
            onDismissRequest = { currentBottomSheet = null },
            sheetState = sheetState
        ) {
            LazyColumn {
                volumePosterItems(state)
            }
        }
    }
}

@Composable
fun MangaStats(
    modifier: Modifier = Modifier,
    ratingItem: @Composable RowScope.(rating: Double) -> Unit = { rating ->
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            val space = LocalSpacing.current
            val text = remember(rating) {
               "%.2f / 10".format(rating)
            }
            Icon(imageVector = Icons.Filled.StarRate, contentDescription = "rating", Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(space.med))
            Text(text, style = MaterialTheme.typography.titleSmall)
        }
    },
    commentsItem: @Composable RowScope.(comments: Int) -> Unit = { comments ->
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            val space = LocalSpacing.current
            Icon(imageVector = Icons.Filled.Comment, contentDescription = "comments", Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(space.med))
            Text(remember(comments) { "$comments" }, style = MaterialTheme.typography.titleSmall)
        }
    },
    followsItem: @Composable RowScope.(follows: Int) -> Unit = { follows ->
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            val space = LocalSpacing.current
            Icon(imageVector = Icons.Filled.Favorite, contentDescription = "follows", Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(space.med))
            Text(remember(follows){ "$follows" }, style = MaterialTheme.typography.titleSmall)
        }
    },
    state: StatsUiState,
) {
    val space = LocalSpacing.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
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

@OptIn(ExperimentalMaterial3Api::class,)
@Composable
fun FilterBottomSheet(
    visible: Boolean,
    filters: Filters,
    sheetState: SheetState,
    onSetAsDefaultClick: () -> Unit,
    filterDownloaded: () -> Unit,
    filterUnread: () -> Unit,
    filterBookmarked: () -> Unit,
    filterBySource: () -> Unit,
    filterByChapterNumber: () -> Unit,
    filterByUploadDate: () -> Unit,
    showSourceTitle: () -> Unit,
    hideSourceTitle: () -> Unit,
    showingSourceTitle: Boolean,
    onDismiss: () -> Unit,
) {
    val space = LocalSpacing.current
    var selectedTabIdx by rememberSaveable {
        mutableIntStateOf(0)
    }
    var dropdownVisible by rememberSaveable {
        mutableStateOf(false)
    }

    if (visible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = {}
        ) {
            TabRow(
                selectedTabIndex = selectedTabIdx,
                modifier = Modifier
                    .padding(space.small)
                    .fillMaxWidth(),
            ) {
                Tab(
                    selected = selectedTabIdx == 0,
                    onClick = { selectedTabIdx = 0 },
                    text = { Text("Filter") }
                )
                Tab(
                    selected = selectedTabIdx == 1,
                    onClick = { selectedTabIdx = 1 },
                    text = {
                        Text("Sort")
                    }
                )
                Tab(
                    selected = selectedTabIdx == 2,
                    onClick = { selectedTabIdx = 2 },
                    text = {
                        Text("Display")
                    }
                )
                Box {
                    IconButton(onClick = { dropdownVisible = !dropdownVisible }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = null
                        )
                    }
                    DropdownMenu(
                        expanded = dropdownVisible,
                        onDismissRequest = { dropdownVisible = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Set as default") },
                            onClick = onSetAsDefaultClick
                        )
                    }
                }
            }
            AnimatedContent(
                targetState = selectedTabIdx,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.25f),
                transitionSpec = {
                       if (initialState < targetState) {
                           slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                       } else {
                           slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                       }
                },
                label = "filter"
            ) { idx ->
                when (idx) {
                    0 -> {
                        val items by remember(filters) {
                            derivedStateOf {
                                listOf(
                                    Triple("Downloaded", filters.downloaded, filterDownloaded),
                                    Triple( "Unread", filters.unread, filterUnread),
                                    Triple("Bookmarked", filters.bookmarked, filterBookmarked)
                                )
                            }
                        }
                        Column(Modifier.fillMaxWidth()) {
                            items.fastForEach { (text, checked, action) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { action() }
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { action() },
                                        enabled = true,
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MaterialTheme.colorScheme.primary,
                                            uncheckedColor = MaterialTheme.colorScheme.onBackground
                                        )
                                    )
                                    Text(text)
                                }
                            }
                        }
                    }
                    1 -> {
                        val items by remember(filters) {
                            derivedStateOf {
                                listOf(
                                    Triple("By source", filters.bySourceAsc, filterBySource),
                                    Triple("By chapter number", filters.byChapterAsc, filterByChapterNumber),
                                    Triple("By upload date", filters.byUploadDateAsc, filterByUploadDate)
                                )
                            }
                        }
                        Column(Modifier.fillMaxWidth()) {
                          items.fastForEach { (text, ascending, action) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { action() }
                                ) {
                                    CenterBox(Modifier.size(42.dp)) {
                                        if (ascending != null) {
                                            IconButton(onClick = action) {
                                                Icon(
                                                    imageVector = if (ascending)
                                                        Icons.Filled.ArrowUpward
                                                    else Icons.Filled.ArrowDownward,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                    Text(text)
                                }
                            }
                        }
                    }
                    2 -> {
                        val items by remember(showingSourceTitle) {
                            derivedStateOf {
                                listOf(
                                    Triple("Source Title", showingSourceTitle, showSourceTitle),
                                    Triple("Chapter number",!showingSourceTitle, hideSourceTitle)
                                )
                            }
                        }
                        Column(Modifier.fillMaxWidth()) {
                           items.fastForEach { (text, selected, action) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { action() }
                                ) {
                                    RadioButton(
                                        selected = selected,
                                        onClick = action
                                    )
                                    Text(text)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterIcon(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val space = LocalSpacing.current
    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.small)) {
            Text("filter")
            IconButton(
                onClick = onClick,
                Modifier
            ) {
                Icon(
                    imageVector = Icons.Filled.FilterList,
                    contentDescription = null
                )
            }
        }
    }
}



