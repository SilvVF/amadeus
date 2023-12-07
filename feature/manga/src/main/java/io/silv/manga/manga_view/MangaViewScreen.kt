package io.silv.manga.manga_view

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import io.silv.datastore.model.Filters
import io.silv.manga.composables.MainPoster
import io.silv.manga.composables.MangaContent
import io.silv.manga.composables.WebViewOverlay
import io.silv.manga.composables.chapterListItems
import io.silv.manga.composables.volumePosterItems
import io.silv.navigation.SharedScreen
import io.silv.navigation.push
import io.silv.ui.collectEvents
import io.silv.ui.composables.AnimatedBoxShimmer
import io.silv.ui.isScrollingUp
import io.silv.ui.locals.LocalNavBarVisibility
import org.koin.core.parameter.parametersOf



class MangaViewScreen(
    private val mangaId: String,
): Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {

        val sm = getScreenModel<MangaViewSM> { parametersOf(mangaId) }
        val mangaViewState = sm.mangaViewStateUiState.collectAsStateWithLifecycle().value
        val downloading by sm.downloadingOrDeleting.collectAsStateWithLifecycle()
        val navigator = LocalNavigator.current
        val statsUiState by sm.statsUiState.collectAsStateWithLifecycle()
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(state = rememberTopAppBarState())
        val listState = rememberLazyListState()
        val snackbarHostState = remember { SnackbarHostState() }

        val navBarChannel = LocalNavBarVisibility.current

        DisposableEffect(Unit) {
            navBarChannel.trySend(false)

            onDispose { navBarChannel.trySend(true) }
        }

        sm.collectEvents { event ->
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
                        message = if (event.bookmarked) "Bookmarked" else "Removed Bookmark",
                        withDismissAction = true,
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    when (result) {
                        SnackbarResult.Dismissed -> Unit
                        SnackbarResult.ActionPerformed -> sm.changeChapterBookmarked(event.id)
                    }
                }
                is MangaViewEvent.ReadStatusChanged -> {
                    val result = snackbarHostState.showSnackbar(
                        message = if (event.read) "Marked as read" else "Marked as unread",
                        withDismissAction = true,
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    when (result) {
                        SnackbarResult.Dismissed -> Unit
                        SnackbarResult.ActionPerformed -> sm.changeChapterReadStatus(event.id)
                    }
                }
            }
        }

        var showArtBottomSheet by rememberSaveable {
            mutableStateOf(false)
        }

        var webUrl by remember {
            mutableStateOf<String?>(null)
        }

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

        if (showArtBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showArtBottomSheet = false },
                sheetState = rememberModalBottomSheetState(
                    skipPartiallyExpanded = true,
                )
            ) {
                LazyColumn {
                    volumePosterItems(mangaViewState)
                }
            }
        }

        var showSourceTitle by rememberSaveable {
            mutableStateOf(true)
        }

        var showFilterBottomSheet by rememberSaveable {
            mutableStateOf(false)
        }

        FilterBottomSheet(
            visible = showFilterBottomSheet,
            filters = mangaViewState.filters,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            onDismiss = { showFilterBottomSheet = false },
            onSetAsDefaultClick = { sm.setFilterAsDefault() },
            filterDownloaded = { sm.filterDownloaded() },
            filterUnread = { sm.filterUnread() },
            filterBookmarked = { sm.filterBookmarked() },
            filterBySource = { sm.filterBySource() },
            filterByChapterNumber = { sm.filterByChapterNumber() },
            filterByUploadDate = { sm.filterByUploadDate() },
            showingSourceTitle = showSourceTitle,
            hideSourceTitle = { showSourceTitle = false },
            showSourceTitle = { showSourceTitle = true }
        )

        Scaffold(
            topBar = {
                val bg = MaterialTheme.colorScheme.background
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = bg
                    ),
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = {
                            navigator?.pop()
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
                    visible = mangaViewState.chapters.fastAny { !it.read },
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    val space = io.silv.ui.theme.LocalSpacing.current
                    ExtendedFloatingActionButton(
                        text = {
                            val text = remember(mangaViewState.chapters) {
                                if (mangaViewState.chapters.fastAny { it.started || it.read }) {
                                    "Resume"
                                } else {
                                    "Start"
                                }
                            }
                            Text(text = text)
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        icon = { Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null) },
                        onClick = {

                            val lastUnread = mangaViewState.chapters
                                .sortedBy { chapter -> chapter.chapter.takeIf { it >= 0 } ?: Long.MAX_VALUE }
                                .fastFirstOrNull { !it.read }
                                ?: mangaViewState.chapters.minByOrNull { it.chapter } ?: return@ExtendedFloatingActionButton

                            navigator?.push(SharedScreen.Reader(lastUnread.mangaId, lastUnread.id))
                        },
                        modifier = Modifier.padding(vertical = space.large),
                        expanded = listState.isScrollingUp(),
                    )
                }
            }
        ) { paddingValues ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                item {
                    when (mangaViewState) {
                        MangaViewState.Loading -> AnimatedBoxShimmer(
                            Modifier
                                .fillMaxWidth()
                                .height(300.dp))
                        is MangaViewState.Success ->  MainPoster(
                            manga = mangaViewState.manga,
                            modifier = Modifier.fillMaxWidth(),
                            viewMangaArtClick = {
                                showArtBottomSheet = !showArtBottomSheet
                            },
                            statsState = statsUiState,
                            padding = paddingValues
                        )
                    }
                }
                item {
                    Column {
                        when (mangaViewState) {
                            MangaViewState.Loading -> {}
                            is MangaViewState.Success -> {
                                MangaContent(
                                    manga = mangaViewState.manga,
                                    bookmarked = mangaViewState.manga.bookmarked,
                                    onBookmarkClicked = sm::bookmarkManga,
                                    onTagSelected = { tag ->
                                        mangaViewState.manga.tagToId[tag]?.let { id ->
                                            navigator?.push(SharedScreen.MangaFilter(tag, id))
                                        }
                                    },
                                    viewOnWebClicked = {
                                        webUrl = "https://mangadex.org/title/${mangaId}"
                                    },
                                    showChapterArt = {
                                        showArtBottomSheet = !showArtBottomSheet
                                    }
                                )
                                FilterIcon(
                                    modifier = Modifier.align(Alignment.End),
                                    onClick = {
                                        showFilterBottomSheet = !showFilterBottomSheet
                                    }
                                )
                            }
                        }
                    }
                }
                chapterListItems(
                    mangaViewState = mangaViewState,
                    downloadingIds = downloading,
                    onDownloadClicked = {
                        sm.downloadChapterImages(it)
                    },
                    onDeleteClicked = {
                        sm.deleteChapterImages(listOf(it))
                    },
                    onReadClicked = {
                        navigator?.push(SharedScreen.Reader(mangaId, it))
                    },
                    onBookmark = sm::changeChapterBookmarked,
                    onMarkAsRead = sm::changeChapterReadStatus,
                    showFullTitle = showSourceTitle,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class
)
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
    val space = io.silv.ui.theme.LocalSpacing.current
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
                                    io.silv.ui.CenterBox(Modifier.size(42.dp)) {
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
    val space = io.silv.ui.theme.LocalSpacing.current
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



