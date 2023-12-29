package io.silv.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.FadeTransition
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.silv.domain.chapter.model.Chapter
import io.silv.navigation.SharedScreen
import io.silv.navigation.push
import io.silv.ui.CenterBox
import io.silv.ui.ReselectTab
import io.silv.ui.collectEvents
import io.silv.ui.composables.Reset
import io.silv.ui.composables.SearchTopAppBar
import io.silv.ui.header
import io.silv.ui.noRippleClickable
import io.silv.ui.theme.LocalSpacing
import io.silv.ui.theme.Pastel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOf

object LibraryTab : ReselectTab {
    private val reselectChannel = Channel<Unit>()

    override suspend fun onReselect(navigator: Navigator) {
        reselectChannel.send(Unit)
    }

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

    @Composable
    override fun Content() {
        Navigator(screen = LibraryScreen()) {
            FadeTransition(navigator = it)
        }
    }
}

enum class LibTab {
    Library, Chapters, Updates, UserLists
}

class LibraryScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
    @Composable
    override fun Content() {

        val screenModel = getScreenModel<LibraryScreenModel>()

        val state by screenModel.state.collectAsStateWithLifecycle()

        val space = LocalSpacing.current
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
            state = rememberTopAppBarState()
        )

        val snackbarHostState = remember { SnackbarHostState() }

        screenModel.collectEvents { event ->
            when (event) {
                is LibraryEvent.BookmarkStatusChanged -> {
                    val result =
                        snackbarHostState.showSnackbar(
                            message = if (event.bookmarked) "Bookmarked" else "Removed Bookmark",
                            withDismissAction = true,
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short,
                        )
                    when (result) {
                        SnackbarResult.Dismissed -> Unit
                        SnackbarResult.ActionPerformed -> screenModel.changeChapterBookmarked(event.id)
                    }
                }
                is LibraryEvent.ReadStatusChanged -> {
                    val result =
                        snackbarHostState.showSnackbar(
                            message = if (event.read) "Marked as read" else "Marked as unread",
                            withDismissAction = true,
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short,
                        )
                    when (result) {
                        SnackbarResult.Dismissed -> Unit
                        SnackbarResult.ActionPerformed -> screenModel.changeChapterReadStatus(event.id)
                    }
                }
            }
        }

        var searching by rememberSaveable { mutableStateOf(false) }
        var searchText by rememberSaveable { mutableStateOf("") }

        var selectedTab by rememberSaveable { mutableStateOf(LibTab.Library) }

        Scaffold(
            modifier =
            Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                SearchTopAppBar(
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
                    onForceSearch = {},
                )
            },
        ) { paddingValues ->

            var filteredItems by remember { mutableStateOf(state.libraryManga) }

            LaunchedEffect(searchText, state.libraryManga) {
                if (searchText.isBlank()) {
                    return@LaunchedEffect
                }
                filteredItems = state.libraryManga.filter {
                    listOf(
                        it.savableManga.titleEnglish,
                        it.savableManga.artists.joinToString(),
                        it.savableManga.authors.joinToString()
                    ).any { string ->
                        searchText.lowercase() in string.lowercase()
                    }
                }
                    .toImmutableList()
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.Transparent,
                ) {
                    LibTab.values().forEach {
                        Tab(
                            selected = selectedTab == it,
                            onClick = { selectedTab = it },
                        ) {
                            Text(text = it.name, modifier = Modifier.padding(space.large))
                        }
                    }
                }
                AnimatedContent(
                    targetState = selectedTab,
                    label = "library-content",
                    modifier = Modifier.padding(space.large),
                    transitionSpec = {
                        if (initialState.ordinal < targetState.ordinal) {
                            slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                        } else {
                            slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                        }
                    },
                ) { selectedTab ->
                    when (selectedTab) {
                        LibTab.Library ->
                            LazyVerticalGrid(
                                modifier = Modifier.weight(1f),
                                columns = GridCells.Fixed(2),
                            ) {
                                if (filteredItems.isEmpty()) {
                                    header {
                                        CenterBox(
                                            Modifier
                                                .fillMaxSize()
                                                .padding(space.med),
                                        ) {
                                            if (searchText.isBlank()) {
                                                Text(
                                                    text = "no manga in your library\nmanga marked as favorite will appear here"
                                                )
                                            } else {
                                                Text(text = "no manga matching given search")
                                            }
                                        }
                                    }
                                } else {
                                    items(
                                        items = filteredItems,
                                        key = { item -> item.savableManga.id },
                                    ) { item ->
                                        LibraryMangaPoster(libraryManga = item)
                                    }
                                }
                            }
                        LibTab.Chapters ->
                            BookmarkedChapterList(
                                bookmarkedChapters = state.bookmarkedChapters,
                                modifier = Modifier.weight(1f),
                                changeChapterRead = screenModel::changeChapterReadStatus,
                                changeChapterBookmarked = screenModel::changeChapterBookmarked,
                                downloadImages = screenModel::downloadChapterImages,
                                deleteImages = screenModel::deleteChapterImages,
                            )
                        LibTab.Updates -> UpdatesList(
                            updates = state.updates,
                            modifier = Modifier.weight(1f)
                        )
                        LibTab.UserLists -> {

                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpdatesList(
    updates: List<Update>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val space = LocalSpacing.current
    val navigator = LocalNavigator.currentOrThrow
    if (updates.isEmpty()) {
        CenterBox(modifier = Modifier.fillMaxSize()) {
            Text(text = "No updates for manga.")
        }
    } else {
        LazyColumn(
            modifier = modifier,
        ) {
            items(updates) {
                when (it) {
                    is Update.Chapter -> {
                        Row(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(space.large)
                                .clickable {
                                    navigator.push(
                                        SharedScreen.Reader(it.manga.id, it.chapterId)
                                    )
                                },
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                model =
                                ImageRequest.Builder(context)
                                    .data(it.manga.coverArt)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier =
                                Modifier
                                    .size(140.dp)
                                    .clickable {
                                        navigator.push(
                                            SharedScreen.MangaView(it.manga.id)
                                        )
                                    },
                            )
                            Column {
                                Text(text = it.manga.titleEnglish)
                                Text(text = "new chapter available")
                            }
                        }
                    }

                    is Update.Volume -> {
                        Row(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(space.large)
                                .clickable {
                                    navigator.push(
                                        SharedScreen.Reader(it.manga.id, it.chapterId)
                                    )
                                },
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                model =
                                ImageRequest.Builder(context)
                                    .data(it.manga.coverArt)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier =
                                Modifier
                                    .size(140.dp)
                                    .clickable {
                                        navigator.push(
                                            SharedScreen.MangaView(it.manga.id)
                                        )
                                    },
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
    bookmarkedChapters: ImmutableList<LibraryChapter>,
    modifier: Modifier,
    changeChapterRead: (id: String) -> Unit,
    changeChapterBookmarked: (id: String) -> Unit,
    downloadImages: (List<String>, String) -> Unit,
    deleteImages: (List<String>, mangaId: String) -> Unit,
) {
    val chaptersByManga by remember {
        derivedStateOf {
            bookmarkedChapters.groupBy { (chapter, _) -> chapter.mangaId }
                .mapKeys { (_, chapter) -> chapter.sortedBy { it.chapter.chapter } }
                .map { (_, v) -> v.toList() }
        }
    }
    val space = LocalSpacing.current
    val navigator = LocalNavigator.currentOrThrow

    LazyColumn(modifier) {
        if (chaptersByManga.isEmpty()) {
            item {
                CenterBox(
                    Modifier
                        .fillMaxSize()
                        .padding(space.med),
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
                            changeChapterRead(chapter.chapter.id)
                        }

                    dismissState.isDismissed(DismissDirection.StartToEnd) ->
                        Reset(dismissState = dismissState) {
                            changeChapterBookmarked(chapter.chapter.id)
                        }
                }
                SwipeToDismiss(
                    state = dismissState,
                    background = {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = space.large),
                            ) {
                                when (dismissState.dismissDirection) {
                                    DismissDirection.StartToEnd ->
                                        Column(
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.align(Alignment.CenterStart),
                                        ) {
                                            Icon(
                                                imageVector =
                                                if (chapter.chapter.bookmarked) {
                                                    Icons.Default.BookmarkRemove
                                                } else {
                                                    Icons.Default.BookmarkAdd
                                                },
                                                contentDescription = "bookmark",
                                            )
                                            Text(
                                                if (chapter.chapter.bookmarked) "Remove bookmark" else "Add bookmark"
                                            )
                                        }

                                    DismissDirection.EndToStart ->
                                        Column(
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.align(Alignment.CenterEnd),
                                        ) {
                                            Icon(
                                                imageVector =
                                                if (chapter.chapter.read) {
                                                    Icons.Default.VisibilityOff
                                                } else {
                                                    Icons.Default.Visibility
                                                },
                                                contentDescription = "read",
                                            )
                                            Text(if (chapter.chapter.read) "Mark unread" else "Mark read")
                                        }

                                    else -> Unit
                                }
                            }
                        }
                    },
                    modifier =
                    Modifier
                        .animateItemPlacement(),
                    dismissContent = {
                        val downloadProgress by chapter.download?.progressFlow
                            ?.collectAsStateWithLifecycle(0)
                            ?: flowOf(0).collectAsState(initial = 0)

                        ChapterListItem(
                            modifier =
                            Modifier
                                .background(MaterialTheme.colorScheme.background)
                                .fillMaxWidth()
                                .padding(
                                    vertical = space.med,
                                    horizontal = space.large,
                                ),
                            chapter = chapter.chapter,
                            downloadProgress = downloadProgress.toFloat(),
                            showFullTitle = true,
                            onDownloadClicked = {
                                downloadImages(listOf(chapter.chapter.id), chapter.chapter.mangaId)
                            },
                            onDeleteClicked = {
                                deleteImages(listOf(chapter.chapter.id), chapter.chapter.mangaId)
                            },
                            onReadClicked = {
                                navigator.push(
                                    SharedScreen.Reader(
                                        mangaId = chapter.chapter.mangaId,
                                        initialChapterId = chapter.chapter.id
                                    )
                                )
                            },
                        )
                    },
                )
            }
            item { Divider() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryMangaPoster(libraryManga: LibraryManga) {
    val (manga, _) = libraryManga
    val ctx = LocalContext.current
    val space = LocalSpacing.current

    CenterBox(
        Modifier.padding(space.large),
    ) {
        val colorPlaceholder =
            remember {
                Pastel.getColorLight()
            }

        AsyncImage(
            model =
            ImageRequest.Builder(ctx)
                .data(manga.coverArt)
                .placeholder(colorPlaceholder)
                .fallback(colorPlaceholder)
                .error(colorPlaceholder)
                .build(),
            contentDescription = null,
            modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .noRippleClickable {
//                    navigator?.push(
//                        MangaViewScreen(manga)
//                    )
                },
        )
        FilterChip(
            onClick = {},
            selected = true,
            modifier =
            Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = -(space.large),
                    y = -(space.large),
                ),
            label = {
                Text(
                    text = libraryManga.unread.toString(),
                )
            },
        )
        IconButton(
            onClick = {
                libraryManga.lastReadChapter?.let {
//                    navigator?.push(
//                        MangaReaderScreen(
//                            mangaId = manga.id,
//                            initialChapterId = it.id
//                        )
//                    )
                }
            },
            modifier =
            Modifier
                .size(18.dp)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Icon(
                imageVector = Icons.Outlined.MenuBook,
                contentDescription = null,
            )
        }
    }
}

@Composable
fun ChapterListItem(
    modifier: Modifier = Modifier,
    showFullTitle: Boolean,
    chapter: Chapter,
    downloadProgress: Float?,
    onDownloadClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onReadClicked: () -> Unit,
) {
    val space = LocalSpacing.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val chapterTitleWithVolText =
            remember(chapter, showFullTitle) {
                if (showFullTitle) {
                    "Chapter ${chapter.chapter.coerceAtLeast(0)}"
                }
                val vol =
                    if (chapter.volume >= 0) {
                        "Vol. ${chapter.volume}"
                    } else {
                        ""
                    }
                "$vol Ch. ${if (chapter.validNumber) chapter.chapter else "extra"} - " + chapter.title
            }
        val dateWithScanlationText =
            remember(chapter) {
                val pageText =
                    if (chapter.lastReadPage > 0 && !chapter.read) {
                        "· Page ${chapter.lastReadPage}"
                    } else {
                        ""
                    }
                "${chapter.daysSinceCreatedString} $pageText · ${chapter.scanlationGroupToId?.first ?: chapter.uploader}"
            }
        Column(
            Modifier
                .padding(space.med)
                .clickable { onReadClicked() }
                .weight(1f),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (chapter.bookmarked) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = "bookmarked",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = space.xs),
                    )
                }
                Text(
                    text = chapterTitleWithVolText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style =
                    MaterialTheme.typography.bodyMedium.copy(
                        color =
                        if (!chapter.read) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            Color.DarkGray
                        },
                    ),
                )
            }
            Spacer(modifier = Modifier.height(space.small))
            Text(
                text = dateWithScanlationText,
                style =
                MaterialTheme.typography.labelLarge.copy(
                    color =
                    if (!chapter.read) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        Color.DarkGray
                    },
                ),
            )
        }
        if (downloadProgress != null) {
            CenterBox {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = null,
                )
                CircularProgressIndicator()
            }
        } else {
            if (chapter.downloaded) {
                IconButton(onClick = { onDeleteClicked() }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                    )
                }
            } else {
                IconButton(onClick = { onDownloadClicked() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowCircleDown,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}
