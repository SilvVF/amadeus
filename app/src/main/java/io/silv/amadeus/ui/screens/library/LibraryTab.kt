package io.silv.amadeus.ui.screens.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDismissState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.FadeTransition
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.silv.amadeus.AmadeusScaffold
import io.silv.amadeus.R
import io.silv.amadeus.ui.screens.manga_reader.MangaReaderScreen
import io.silv.amadeus.ui.screens.manga_view.MangaViewScreen
import io.silv.amadeus.ui.screens.manga_view.composables.ChapterListItem
import io.silv.amadeus.ui.screens.manga_view.composables.Reset
import io.silv.model.SavableChapter
import io.silv.ui.collectEvents
import io.silv.ui.header
import io.silv.ui.noRippleClickable
import io.silv.ui.theme.Pastel

object LibraryTab: Tab {

    @OptIn(ExperimentalAnimationGraphicsApi::class)
    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_library_enter)
            return TabOptions(
                index = 2u,
                title = "Library",
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    override fun Content() {
        Navigator(screen = LibraryScreen()) {
            FadeTransition(navigator = it)
        }
    }
}

class LibraryScreen: Screen {

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class,
        ExperimentalFoundationApi::class
    )
    @Composable
    override fun Content() {

        val sm = getScreenModel<LibrarySM>()
        val mangasToChapters by sm.mangaWithDownloadedChapters.collectAsStateWithLifecycle()
        val space = io.silv.ui.theme.LocalSpacing.current
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(state = rememberTopAppBarState())
        val bookmarkedChapters by sm.bookmarkedChapters.collectAsStateWithLifecycle()
        val downloadingOrDeletingIds by sm.downloadingOrDeleting.collectAsStateWithLifecycle()
        val updatedManga by sm.updates.collectAsStateWithLifecycle()
        val navigator = LocalNavigator.current
        val snackbarHostState = remember { SnackbarHostState() }

        sm.collectEvents { event ->
            when (event) {
                is LibraryEvent.BookmarkStatusChanged -> {
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
                is LibraryEvent.ReadStatusChanged -> {
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

        var searching by rememberSaveable {
            mutableStateOf(false)
        }

        var searchText by rememberSaveable {
            mutableStateOf("")
        }

        var selectedTab by rememberSaveable {
            mutableStateOf(0)
        }

        AmadeusScaffold(
            modifier = Modifier.fillMaxSize(),
            scrollBehavior = scrollBehavior,
            topBar = {
                io.silv.explore.composables.SearchTopAppBar(
                    title = "Library",
                    scrollBehavior = scrollBehavior,
                    onSearchText = { searchText = it },
                    color = Color.Transparent,
                    navigationIconLabel = "",
                    navigationIcon = Icons.Filled.KeyboardArrowLeft,
                    onNavigationIconClicked = { searching = false },
                    actions = {},
                    searchText = searchText,
                    showTextField = searching,
                    onSearchChanged = { searching = it },
                    onForceSearch = {}
                )
            }
        ) { paddingValues ->

            val filteredItems by remember(searchText, mangasToChapters) {
                derivedStateOf {
                    if (searchText.isBlank()) { return@derivedStateOf mangasToChapters }
                    mangasToChapters.filter {
                        listOf(
                            it.savableManga.titleEnglish,
                            it.savableManga.artists.joinToString(),
                            it.savableManga.authors.joinToString()
                        ).any { string ->
                            searchText.lowercase() in string.lowercase()
                        }
                    }
                }
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.Transparent
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    ) {
                        Text(text = "Manga", modifier = Modifier.padding(space.large))
                    }
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    ) {
                        Text(text = "Chapters", modifier = Modifier.padding(space.large))
                    }
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 }
                    ) {
                        Text(text = "Updates", modifier = Modifier.padding(space.large))
                    }
                }
                AnimatedContent(
                    targetState = selectedTab,
                    label = "library-content",
                    modifier = Modifier.padding(space.large),
                    transitionSpec = {
                        if (initialState < targetState) {
                            slideInHorizontally { it } with slideOutHorizontally { -it }
                        } else {
                            slideInHorizontally { -it } with slideOutHorizontally { it }
                        }
                    }
                ) { selectedTab ->
                    when (selectedTab) {
                        0 -> LazyVerticalGrid(
                            modifier = Modifier.weight(1f),
                            columns = GridCells.Fixed(2),
                        ) {
                            if (filteredItems.isEmpty()) {
                                header {
                                    io.silv.ui.CenterBox(
                                        Modifier
                                            .fillMaxSize()
                                            .padding(space.med)
                                    ) {
                                        if (searchText.isBlank()) {
                                            Text(text = "no manga in your library\nmanga marked as favorite will appear here")
                                        } else {
                                            Text(text = "no manga matching given search")
                                        }
                                    }
                                }
                            } else {
                                items(
                                    items = filteredItems,
                                    key = { item -> item.savableManga.id }
                                ) {item ->
                                    LibraryMangaPoster(libraryManga = item)
                                }
                            }
                        }
                        1 -> BookmarkedChapterList(
                            bookmarkedChapters = bookmarkedChapters,
                            modifier = Modifier.weight(1f),
                            changeChapterRead = sm::changeChapterReadStatus,
                            changeChapterBookmarked = sm::changeChapterBookmarked,
                            downloadingOrDeletingIds = downloadingOrDeletingIds,
                            downloadImages = sm::downloadChapterImages,
                            deleteImages = sm::deleteChapterImages
                        )
                        else -> UpdatesList(updates = updatedManga, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun UpdatesList(
    updates: List<Update>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val space = io.silv.ui.theme.LocalSpacing.current
    val navigator = LocalNavigator.current
    if (updates.isEmpty()) {
        io.silv.ui.CenterBox(modifier = Modifier.fillMaxSize()) {
            Text(text = "No updates for manga.")
        }
    } else {
        LazyColumn(
            modifier = modifier
        ) {
            items(updates) {
                when (it) {
                    is Update.Chapter -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(space.large)
                                .clickable {
                                    navigator?.push(
                                        MangaReaderScreen(it.manga.id, it.chapterId)
                                    )
                                },
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(it.manga.coverArt)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(140.dp)
                                    .clickable {
                                        navigator?.push(
                                            MangaViewScreen(it.manga)
                                        )
                                    }
                            )
                            Column {
                                Text(text = it.manga.titleEnglish)
                                Text(text = "new chapter available")
                            }
                        }
                    }

                    is Update.Volume -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(space.large)
                                .clickable {
                                    navigator?.push(
                                        MangaReaderScreen(it.manga.id, it.chapterId)
                                    )
                                },
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(it.manga.coverArt)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(140.dp)
                                    .clickable {
                                        navigator?.push(
                                            MangaViewScreen(it.manga)
                                        )
                                    }
                            )
                            Column {
                                Text(text = it.manga.titleEnglish)
                                Text(text = "new volume available")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BookmarkedChapterList(
    bookmarkedChapters: List<SavableChapter>,
    modifier: Modifier,
    changeChapterRead: (id: String) -> Unit,
    changeChapterBookmarked: (id: String) -> Unit,
    downloadingOrDeletingIds: List<Pair<String, Float>>,
    downloadImages: (List<String>, String) -> Unit,
    deleteImages: (List<String>) -> Unit
) {
    val chaptersByManga by remember {
        derivedStateOf {
            bookmarkedChapters.groupBy { chapter -> chapter.mangaId }
                .mapKeys { (k, chapter) -> chapter.sortedBy { it.chapter } }
                .map { (k, v) -> v.toList() }
        }
    }
    val space = io.silv.ui.theme.LocalSpacing.current
    val navigator = LocalNavigator.current
    LazyColumn(modifier) {
        if (chaptersByManga.isEmpty()) {
            item {
                io.silv.ui.CenterBox(
                    Modifier
                        .fillMaxSize()
                        .padding(space.med)
                ) {

                    Text(text = "no chapters have been bookmarked yet")
                }
            }
        }
        for (chapters in chaptersByManga) {
            items(chapters) { chapter ->
                val dismissState = rememberDismissState()
                when {
                    dismissState.isDismissed(DismissDirection.EndToStart) ->
                        Reset(dismissState = dismissState) {
                            changeChapterRead(chapter.id)
                        }

                    dismissState.isDismissed(DismissDirection.StartToEnd) ->
                        Reset(dismissState = dismissState) {
                            changeChapterBookmarked(chapter.id)
                        }
                }
                SwipeToDismiss(
                    state = dismissState,
                    background = {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = space.large)
                            ) {
                                when (dismissState.dismissDirection) {
                                    DismissDirection.StartToEnd -> Column(
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.align(Alignment.CenterStart)
                                    ) {
                                        Icon(
                                            imageVector = if (chapter.bookmarked)
                                                Icons.Default.BookmarkRemove
                                            else Icons.Default.BookmarkAdd,
                                            contentDescription = "bookmark"
                                        )
                                        Text(if (chapter.bookmarked) "Remove bookmark" else "Add bookmark")
                                    }

                                    DismissDirection.EndToStart -> Column(
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.align(Alignment.CenterEnd)
                                    ) {
                                        Icon(
                                            imageVector = if (chapter.read)
                                                Icons.Default.VisibilityOff
                                            else Icons.Default.Visibility,
                                            contentDescription = "read"
                                        )
                                        Text(if (chapter.read) "Mark unread" else "Mark read")
                                    }

                                    else -> Unit
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .animateItemPlacement(),
                    dismissContent = {
                        ChapterListItem(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.background)
                                .fillMaxWidth()
                                .padding(
                                    vertical = space.med,
                                    horizontal = space.large
                                ),
                            chapter = chapter,
                            downloadProgress = downloadingOrDeletingIds.fastFirstOrNull { it.first == chapter.id }?.second,
                            showFullTitle = true,
                            onDownloadClicked = {
                                downloadImages(listOf(chapter.id), chapter.mangaId)
                            },
                            onDeleteClicked = {
                                deleteImages(listOf(chapter.id))
                            },
                            onReadClicked = {
                                navigator?.push(
                                    MangaReaderScreen(
                                        chapter.mangaId,
                                        chapter.id
                                    )
                                )
                            }
                        )
                    }
                )
            }
            item { Divider() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryMangaPoster(
    libraryManga: LibraryManga
) {
    val (manga, chapters) = libraryManga
    val ctx = LocalContext.current
    val space = io.silv.ui.theme.LocalSpacing.current
    val navigator = LocalNavigator.current
    io.silv.ui.CenterBox(
        Modifier.padding(space.large)
    ) {
        val colorPlaceholder = remember {
            Pastel.getColorLight()
        }

        AsyncImage(
            model = ImageRequest.Builder(ctx)
                .data(manga.coverArt)
                .placeholder(colorPlaceholder)
                .fallback(colorPlaceholder)
                .error(colorPlaceholder)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .noRippleClickable {
                    navigator?.push(
                        MangaViewScreen(manga)
                    )
                }
        )
        FilterChip(
            onClick = {},
            selected = true,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = -(space.large),
                    y = -(space.large)
                ),
            label = {
                Text(
                    text = libraryManga.unread.toString()
                )
            }
        )
        IconButton(
            onClick = {
                libraryManga.lastReadChapter?.let {
                    navigator?.push(
                        MangaReaderScreen(
                            mangaId = manga.id,
                            initialChapterId = it.id
                        )
                    )
                }
            },
            modifier = Modifier
                .size(18.dp)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)

        ) {
            Icon(
                imageVector = Icons.Outlined.MenuBook,
                contentDescription = null
            )
        }
    }
}