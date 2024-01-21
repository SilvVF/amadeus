@file:OptIn(ExperimentalMaterial3Api::class)

package io.silv.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UpdateDisabled
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.twotone.Archive
import androidx.compose.material.icons.twotone.Unarchive
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import coil.compose.AsyncImage
import io.silv.common.model.Download
import io.silv.common.model.MangaCover
import io.silv.common.time.localDateTimeNow
import io.silv.datastore.LibraryPrefs
import io.silv.datastore.collectAsState
import io.silv.domain.chapter.model.Chapter
import io.silv.domain.manga.model.Manga
import io.silv.domain.manga.model.MangaWithChapters
import io.silv.domain.update.UpdateWithRelations
import io.silv.library.state.LibraryActions
import io.silv.library.state.LibraryError
import io.silv.library.state.LibraryMangaState
import io.silv.library.state.LibraryState
import io.silv.navigation.SharedScreen
import io.silv.navigation.push
import io.silv.ui.CenterBox
import io.silv.ui.Converters
import io.silv.ui.LocalAppState
import io.silv.ui.ReselectTab
import io.silv.ui.composables.CardType
import io.silv.ui.composables.ChapterDownloadAction
import io.silv.ui.composables.ChapterDownloadIndicator
import io.silv.ui.composables.ChapterListItem
import io.silv.ui.composables.MangaGridItem
import io.silv.ui.composables.MangaListItem
import io.silv.ui.composables.PullRefresh
import io.silv.ui.composables.SearchTextField
import io.silv.ui.conditional
import io.silv.ui.layout.ExpandableInfoLayout
import io.silv.ui.layout.ExpandableState
import io.silv.ui.layout.ScrollbarLazyColumn
import io.silv.ui.layout.TopAppBarWithBottomContent
import io.silv.ui.layout.rememberExpandableState
import io.silv.ui.theme.AmadeusTheme
import io.silv.ui.theme.LocalSpacing
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.number
import kotlinx.parcelize.IgnoredOnParcel
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox

object LibraryTab : ReselectTab {

    @IgnoredOnParcel
    internal val reselectChannel = Channel<Unit>()

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
                index = 1u,
                title = "Library",
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    @Composable
    override fun Content() {

        val appState = LocalAppState.current

        val screenModel = getScreenModel<LibraryScreenModel>()

        val state by screenModel.state.collectAsStateWithLifecycle()

        val lifeCycleOwner = LocalLifecycleOwner.current
        val expandableState = rememberExpandableState()

        LaunchedEffect(reselectChannel, lifeCycleOwner) {
            lifeCycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                withContext(Dispatchers.Main.immediate) {
                    reselectChannel.receiveAsFlow().collectLatest {
                        expandableState.toggleProgress()
                    }
                }
            }
        }

        LibraryScreenContent(
            stateProvider = { state },
            expandableState = expandableState,
            searchTextProvider = { screenModel.mangaSearchText },
            actions = LibraryActions(
                searchChanged = screenModel::onSearchChanged,
                filterByTag = screenModel::onTagFiltered,
                clearTagFilter = {
                    state.libraryMangaState.success?.filteredTagIds?.fastForEach(screenModel::onTagFiltered)
                },
                searchOnMangaDex = {
                    appState.searchGlobal(it)
                },
                navigateToExploreTab = {
                    appState.searchGlobal(it)
                },
                onDownload = screenModel::startDownload,
                onCancelDownload = screenModel::cancelDownload,
                onStartDownloadNow = screenModel::startDownloadNow,
                onDeleteDownloadedChapter = screenModel::deleteDownloadedChapter,
                refreshUpdates = screenModel::refreshLibrary,
                toggleChapterBookmark = screenModel::toggleChapterBookmark,
                toggleChapterRead = screenModel::toggleChapterRead,
                markUpdatesAsSeen = screenModel::updateMangaUpdatedTrackedAfter,
                pauseAllDownloads = screenModel::pauseAllDownloads
            )
        )
    }
}

enum class LibTab {
    Library, Chapters, Updates
}

enum class LibraryBottomSheet {
    DisplayOptions, Filters
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTopAppBar(
    searchText: () -> String,
    actions: LibraryActions,
    onOptionsClick: () -> Unit,
    onTabSelected: (LibTab) -> Unit,
    selectedTabProvider: () -> LibTab,
    scrollBehavior: TopAppBarScrollBehavior,
) {

    val space = LocalSpacing.current

    var searching by rememberSaveable { mutableStateOf(false) }
    var alreadyRequestedFocus by rememberSaveable { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    TopAppBarWithBottomContent(
        scrollBehavior = scrollBehavior,
        title = {
            AnimatedContent(
                targetState = searching,
                transitionSpec = {
                    if (this.targetState) { // animate search text in from top out to top
                        fadeIn() + slideInVertically { -it } togetherWith fadeOut() + slideOutVertically { it }
                    } else { // animate title up from bottom out to bottom
                        fadeIn() + slideInVertically { it } togetherWith fadeOut() + slideOutVertically { -it }
                    }
                },
                label = "search-anim",
            ) { targetState ->
                if (targetState) {
                    LaunchedEffect(Unit) {
                        if (!alreadyRequestedFocus && searching) {
                            focusRequester.requestFocus()
                        } else {
                            alreadyRequestedFocus = false
                        }
                    }
                    SearchTextField(
                        searchText = searchText(),
                        placeHolder = {
                            Text(
                                text =  remember(selectedTabProvider()) {
                                    "Search ${selectedTabProvider.invoke()}..."
                                }
                            )
                        },
                        onValueChange = {
                            actions.searchChanged(it)
                        },
                        onSearch = {
                            focusRequester.freeFocus()
                        },
                        focusRequester = focusRequester,
                    )
                } else {
                    AnimatedContent(
                        targetState = selectedTabProvider(),
                        label = "tab-text",
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        }
                    ) { tab ->
                        Text(
                            text = remember(tab) { tab.toString() }
                        )
                    }
                }
            }
        },
        actions = {
            val icon =
                when (searching) {
                    true -> Icons.Filled.SearchOff
                    false -> Icons.Filled.Search
                }
            IconButton(
                onClick = {
                    alreadyRequestedFocus = false
                    searching = !searching
                },
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                )
            }
            IconButton(onClick = onOptionsClick) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = "Display Options",
                )
            }
        },
        bottomContent = {
            val tabs = remember { LibTab.entries }
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                val selectedTab = selectedTabProvider()
                tabs.fastForEach {
                    ElevatedFilterChip(
                        selected = it == selectedTab,
                        onClick = { onTabSelected(it) },
                        label = { Text(it.name, maxLines = 1) },
                        modifier = Modifier.padding(space.small)
                    )
                }
            }
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreenContent(
    stateProvider: () -> LibraryState,
    expandableState: ExpandableState = rememberExpandableState(),
    searchTextProvider: () -> String,
    actions: LibraryActions
) {

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val space = LocalSpacing.current
    val scope = rememberCoroutineScope()

    var currentTab by rememberSaveable { mutableStateOf(LibTab.Library) }
    var currentBottomSheet: LibraryBottomSheet? by remember { mutableStateOf(null) }

    when (currentBottomSheet) {
        LibraryBottomSheet.DisplayOptions -> LibraryOptionsBottomSheet {
            currentBottomSheet = null
        }
        LibraryBottomSheet.Filters -> Unit
        null -> Unit
    }

    Scaffold(
        topBar = {
            LibraryTopAppBar(
                actions = actions,
                onOptionsClick = {
                    scope.launch {
                        expandableState.toggleProgress()
                    }
                },
                searchText = searchTextProvider,
                onTabSelected = { currentTab = it },
                selectedTabProvider = { currentTab },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val mangaState = stateProvider().libraryMangaState) {
                LibraryMangaState.Loading -> {
                    CenterBox(Modifier.fillMaxSize()) {
                        CircularProgressIndicator()
                    }
                }
                is LibraryMangaState.Error -> {
                    ErrorScreenContent(
                        state = mangaState,
                        query = searchTextProvider(),
                        paddingValues = paddingValues,
                        navigateToExploreTab = {
                            actions.navigateToExploreTab(searchTextProvider())
                        }
                    )
                }
                is LibraryMangaState.Success -> {
                    SuccessScreenContent(
                        state = stateProvider(),
                        mangaState = mangaState,
                        paddingValues,
                        currentTab,
                        actions
                    )
                }
            }
            ExpandableInfoLayout(
                state = expandableState,
                modifier = Modifier.align(Alignment.BottomCenter),
                peekContent = {
                    LibraryPeekContent(
                        stateProvider = stateProvider,
                        actions = actions
                    )
                }
            ) {
                val surfaceColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                    BottomSheetDefaults.Elevation
                )
                Column(
                    Modifier
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .drawBehind {
                            drawRect(color = surfaceColor)
                        }
                        .padding(space.small),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { currentBottomSheet = LibraryBottomSheet.DisplayOptions }
                            .padding(space.med),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Display options",
                        )
                        Spacer(modifier = Modifier.width(space.med))
                        Text(text = "Display options")
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorScreenContent(
    state: LibraryMangaState.Error,
    query: String,
    navigateToExploreTab: () -> Unit,
    paddingValues: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(paddingValues),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.HeartBroken,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(
                    (LocalConfiguration.current.screenHeightDp * 0.2f).dp
                ),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = buildAnnotatedString {
                val libEmpty = "Your library is currently empty. "
                val s = if (query.isNotBlank()) {
                    "Search for "
                } else {
                    "Add manga using the "
                }
                val start = libEmpty + s
                val tab = "Explore Tab"
                val end = if (query.isNotBlank()) {
                    " using "
                } else {
                    " "
                }

                addStyle(
                    style = MaterialTheme.typography.labelLarge
                        .toSpanStyle().copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    start = start.length,
                    end = start.length + query.length
                )

                addStyle(
                    style = MaterialTheme.typography.labelLarge.toSpanStyle(),
                    start = start.length + query.length,
                    start.length + query.length + end.length
                )
                addStyle(
                    style = MaterialTheme.typography.labelLarge
                        .toSpanStyle().copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    start.length + query.length + end.length,
                    start.length + query.length + end.length + tab.length
                )
                append(start)
                append(query)
                append(end)
                append(tab)
            },
            modifier = Modifier
                .clip(CircleShape)
                .padding(12.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(color = MaterialTheme.colorScheme.primary)
                ) {
                    navigateToExploreTab()
                },
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryPeekContent(
    modifier: Modifier = Modifier,
    stateProvider: () -> LibraryState,
    actions: LibraryActions,
) {

    val space = LocalSpacing.current
    val state = stateProvider()

    LazyRow(
        modifier = modifier
    ) {
        state.libraryMangaState.success?.libraryTags?.let { tags ->
            if(tags.fastAny { it.selected } && !tags.fastAll { it.selected }) {
                item(
                    key = "clear"
                ) {
                    AssistChip(
                        onClick = actions.clearTagFilter,
                        label = { Text("clear") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Clear ,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier
                            .animateItemPlacement()
                            .padding(space.small)
                    )
                }
            }
            tags.fastForEach { (tag, id, selected) ->
                item(
                    contentType = "Tag",
                    key = id
                ) {
                    ElevatedFilterChip(
                        selected = selected,
                        onClick = { actions.filterByTag(id) },
                        label = { Text(tag) },
                        modifier = Modifier
                            .animateItemPlacement()
                            .padding(space.small)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SuccessScreenContent(
    state: LibraryState,
    mangaState: LibraryMangaState.Success,
    paddingValues: PaddingValues,
    currentTab: LibTab,
    actions: LibraryActions,
) {
    val space = LocalSpacing.current

    val showGlobalSearch by remember(state) {
        derivedStateOf {
            mangaState.filteredTagIds.isEmpty() &&
                    mangaState.filteredMangaWithChapters.isEmpty() &&
                    mangaState.filteredText.isNotBlank()
        }
    }

    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.currentOrThrow

    val cardType by LibraryPrefs.cardTypePrefKey.collectAsState(
        defaultValue = CardType.Compact,
        converter = Converters.CardTypeToStringConverter,
        scope = scope,
    )

    val gridCells by LibraryPrefs.gridCellsPrefKey.collectAsState(LibraryPrefs.gridCellsDefault, scope)
    val useList by LibraryPrefs.useListPrefKey.collectAsState(false, scope)
    val animatePlacement by LibraryPrefs.animatePlacementPrefKey.collectAsState(true, scope)

    AnimatedContent(
        targetState = currentTab,
        label = "curr-tab",
        transitionSpec = {
            if (this.targetState.ordinal < this.initialState.ordinal) {
                slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
            } else {
                slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {targetStateTab ->
        when (targetStateTab) {
            LibTab.Library -> LibraryManga(
                useList = useList,
                cardType = cardType,
                gridCells = gridCells,
                animatePlacement = animatePlacement,
                paddingValues = paddingValues,
                showGlobalSearch = showGlobalSearch,
                state = mangaState,
                actions = actions
            )
            LibTab.Chapters -> BookmarkedChapters(
                paddingValues = paddingValues,
                state = state,
                actions = actions
            )
            LibTab.Updates -> UpdatesList(
                state = state,
                actions = actions,
                paddingValues = paddingValues
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UpdatesList(
    state: LibraryState,
    actions: LibraryActions,
    paddingValues: PaddingValues,
) {
    val space = LocalSpacing.current
    val navigator = LocalNavigator.currentOrThrow

    val timeText = remember(state.libraryLastUpdated) {
        state.libraryLastUpdated?.let {

            var h = it.time.hour
            var isPm = false

            when (h) {
                0 -> h = 12
                12 -> isPm = true
                in 12..24 -> {
                    h -= 12
                    isPm = true
                }
            }
            val amPm = if (isPm) "PM" else "AM"
            val time = if (it.time.minute < 10) {
                "0${it.time.minute}"
            } else it.time.minute.toString()

            "Library last updated at ${it.date.month.number}/${it.date.dayOfMonth}/${it.date.year} - $h:${time} $amPm"
        }
    }

    PullRefresh(
        refreshing = state.updatingLibrary,
        onRefresh = { actions.refreshUpdates() },
        paddingValues = paddingValues
    ) {
        if (state.updates.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.UpdateDisabled,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surfaceTint,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
                Text(
                    "No updates",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium
                )
                timeText?.let {
                    Text(
                        it,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = paddingValues
            ) {
                item(key = "last-updated") {
                    timeText?.let {
                        Text(
                            it,
                            textAlign = TextAlign.Start,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                state.updates.fastForEach { (epochDay, updatesList) ->
                    item(key = epochDay) {
                        val text = remember {
                            when (val dif = localDateTimeNow().date.toEpochDays() - epochDay) {
                                0 -> "Today"
                                1 -> "Yesterday"
                                else -> "$dif days ago"
                            }
                        }
                        Text(
                            text,
                            style = MaterialTheme.typography.labelLarge
                                .copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier
                                .animateItemPlacement()
                                .padding(space.med)
                        )
                    }
                    items(
                        updatesList,
                        key = { it.update.chapterId }) { (update, downloaded, download) ->
                        val markSeen = SwipeAction(
                            onSwipe = {
                                actions.markUpdatesAsSeen(
                                    update.mangaId,
                                    update.chapterId
                                )
                            },
                            icon = rememberVectorPainter(image = Icons.Outlined.VisibilityOff),
                            background = MaterialTheme.colorScheme.surfaceTint
                        )

                        SwipeableActionsBox(
                            endActions = listOf(markSeen),
                            modifier = Modifier.animateItemPlacement()
                        ) {
                            MangaUpdateItem(
                                update = update,
                                download = download,
                                downloaded = downloaded,
                                onCoverClick = {
                                    navigator.push(
                                        SharedScreen.MangaView(update.mangaId)
                                    )
                                },
                                onClick = {
                                    navigator.push(
                                        SharedScreen.Reader(update.mangaId, update.chapterId)
                                    )
                                },
                                onDownloadAction = { action ->
                                    when (action) {
                                        ChapterDownloadAction.START ->
                                            actions.onDownload(update.mangaId, update.chapterId)

                                        ChapterDownloadAction.START_NOW ->
                                            download?.let { actions.onStartDownloadNow(it) }

                                        ChapterDownloadAction.CANCEL ->
                                            download?.let { actions.onCancelDownload(it) }

                                        ChapterDownloadAction.DELETE ->
                                            actions.onDeleteDownloadedChapter(
                                                update.mangaId,
                                                update.chapterId
                                            )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyItemScope.MangaUpdateItem(
    download: Download?,
    downloaded: Boolean,
    update: UpdateWithRelations,
    onCoverClick: () -> Unit,
    onClick: () -> Unit,
    onDownloadAction: (ChapterDownloadAction) -> Unit,
    preview: Boolean = false
) {
    val space = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(space.med)
            .animateItemPlacement()
            .clickable {
                onClick()
            },
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = MangaCover(
                update.mangaId,
                update.coverArt,
                update.favorite,
                update.coverLastModified
            )
                .takeIf { !preview } ?: update.coverArt,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height(90.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    onCoverClick()
                }
        )
        Spacer(modifier = Modifier.width(space.large))
        Column(Modifier.weight(1f)) {
            Text(
                text = update.chapterName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 2
            )
            Text(
                text = "Ch. ${update.chapterNumber} -" +
                        " ${update.scanlator}",
                maxLines = 1,
                style = MaterialTheme.typography.labelLarge
            )
        }
        Spacer(modifier = Modifier.width(space.large))

        val status by remember(download) {
            download?.statusFlow
                ?: flowOf(
                    if (downloaded) Download.State.DOWNLOADED else Download.State.NOT_DOWNLOADED
                )
        }
            .collectAsState(Download.State.NOT_DOWNLOADED)

        val progress by remember(download) {
            download?.progressFlow ?: flowOf(0)
        }
            .collectAsState(0)

        ChapterDownloadIndicator(
            enabled = true,
            downloadStateProvider = { status },
            downloadProgressProvider = { progress },
            onClick = onDownloadAction
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookmarkedChapters(
    paddingValues: PaddingValues,
    state: LibraryState,
    actions: LibraryActions,
) {
    val navigator = LocalNavigator.currentOrThrow

    if (state.bookmarkedChapters.isEmpty()) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.BookmarkBorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surfaceTint,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.2f)
            )
            Text(
                "No chapters bookmarked",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium
            )
        }
    } else {
        ScrollbarLazyColumn(
            contentPadding = paddingValues
        ) {
            state.bookmarkedChapters.fastForEach { (manga, chapters) ->
                item(key = "${manga.id}-header") {
                    Text(
                        text = manga.titleEnglish,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                items(chapters, key = { it.id }) { chapter ->
                    val space = LocalSpacing.current

                    val archive =
                        SwipeAction(
                            icon =
                            rememberVectorPainter(
                                if (chapter.bookmarked) {
                                    Icons.TwoTone.Archive
                                } else {
                                    Icons.TwoTone.Unarchive
                                },
                            ),
                            background = MaterialTheme.colorScheme.primary,
                            isUndo = chapter.bookmarked,
                            onSwipe = {
                                actions.toggleChapterBookmark(chapter.id)
                            },
                        )

                    val read =
                        SwipeAction(
                            icon =
                            rememberVectorPainter(
                                if (chapter.read) {
                                    Icons.Filled.VisibilityOff
                                } else {
                                    Icons.Filled.VisibilityOff
                                },
                            ),
                            background = MaterialTheme.colorScheme.primary,
                            isUndo = chapter.read,
                            onSwipe = {
                                actions.toggleChapterRead(chapter.id)
                            },
                        )

                    SwipeableActionsBox(
                        startActions = listOf(archive),
                        endActions = listOf(read),
                        modifier = Modifier.animateItemPlacement()
                    ) {
                        ChapterListItem(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navigator.push(
                                        SharedScreen.Reader(manga.id, chapter.id)
                                    )
                                }
                                .padding(
                                    vertical = space.med,
                                    horizontal = space.large,
                                ),
                            chapter = chapter,
                            download = null,
                            showFullTitle = true,
                            onDownloadClicked = { actions.onDownload(chapter.mangaId, chapter.id) },
                            onDeleteClicked = {
                                actions.onDeleteDownloadedChapter(chapter.mangaId, chapter.id)
                            },
                            onCancelClicked = {
                                actions.onCancelDownload(it)
                            },
                            onPauseClicked = {
                                actions.pauseAllDownloads()
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryManga(
    useList: Boolean,
    cardType: CardType,
    gridCells: Int,
    animatePlacement: Boolean,
    paddingValues: PaddingValues,
    showGlobalSearch: Boolean,
    state: LibraryMangaState.Success,
    actions: LibraryActions,
) {

    val space = LocalSpacing.current
    val navigator = LocalNavigator.currentOrThrow

    if (useList) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues
        ) {
            if (showGlobalSearch) {
                item(
                    key = "search-global",
                ) {
                    Text(
                        text = "Search for \"${state.filteredText}\" on MangaDex",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.primary
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                actions.searchOnMangaDex(state.filteredText)
                            }
                    )
                }
            }
            items(
                items = state.filteredMangaWithChapters,
                key = { (manga, _) -> manga.id }
            ) { (manga, _) ->
                MangaListItem(
                    manga = manga,
                    modifier = Modifier
                        .conditional(animatePlacement) {
                            animateItemPlacement()
                        }
                        .padding(space.small),
                    onClick = {
                        navigator.push(SharedScreen.MangaView(it.id))
                    },
                    onFavoriteClick = {},
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridCells),
            contentPadding = paddingValues,
            modifier = Modifier.fillMaxSize()
        ) {
            if (showGlobalSearch) {
                item(
                    key = "search-global",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    Text(
                        text = "Search for \"${state.filteredText}\" on MangaDex",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.primary
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                actions.searchOnMangaDex(state.filteredText)
                            }
                    )
                }
            }
            items(
                items = state.filteredMangaWithChapters,
                key = { (manga, _) -> manga.id },
            ) { (manga, _) ->
                MangaGridItem(
                    manga = manga,
                    modifier = Modifier
                        .clickable {
                            navigator.push(SharedScreen.MangaView(manga.id))
                        }
                        .conditional(animatePlacement) {
                            animateItemPlacement()
                        }
                        .padding(space.small)
                        .aspectRatio(2f / 3f),
                    onTagClick = {},
                    onBookmarkClick = {},
                    cardType = cardType
                )
            }
        }
    }
}


@Preview
@Composable
fun LibraryScreenErrorNoFavoritedContentPreview() {

    var searchText by rememberSaveable {
        mutableStateOf("")
    }

    val state = remember {
        LibraryState().copy(
            libraryMangaState = LibraryMangaState.Error(LibraryError.NoFavoritedChapters)
        )
    }

    AmadeusTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            LibraryScreenContent(
                stateProvider = { state },
                searchTextProvider = { searchText },
                expandableState = rememberExpandableState(),
                actions = LibraryActions(
                    filterByTag = {

                    },
                    clearTagFilter = {

                    },
                    searchChanged = {
                        searchText = it
                    }
                )
            )
        }
    }
}

@Preview
@Composable
fun LibraryScreeGenericErrorPreview() {

    var searchText by rememberSaveable {
        mutableStateOf("")
    }

    val state = remember {
        LibraryState(
            libraryMangaState = LibraryMangaState.Error(LibraryError.Generic("dkjfakljdf"))
        )
    }

    AmadeusTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            LibraryScreenContent(
                stateProvider = { state },
                searchTextProvider = { searchText },
                actions = LibraryActions(
                    filterByTag = {

                    },
                    clearTagFilter = {

                    },
                    searchChanged = {
                        searchText = it
                    }
                )
            )
        }
    }
}

@Preview
@Composable
fun LibraryScreenContentSuccessPreview() {

    var state by remember {
        mutableStateOf(
            LibraryState(
                libraryMangaState = LibraryMangaState.Success(
                    mangaWithChapters = buildList {
                        repeat(5) {
                            val manga = Manga.stub()
                            add(
                                MangaWithChapters(
                                    manga = manga,
                                    chapters = buildList {
                                        repeat(30) {
                                            add(Chapter.stub(manga.id, 1, it.toDouble()))
                                        }
                                    }
                                        .toImmutableList()
                                )
                            )
                        }
                    }.toImmutableList()
                )
            )
        )
    }
    AmadeusTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            LibraryScreenContent(
                stateProvider = { state },
                searchTextProvider = { state.libraryMangaState.success?.filteredText ?: "" },
                actions = LibraryActions(
                    filterByTag = {

                    },
                    clearTagFilter = {

                    },
                    searchChanged = { _ ->

                    }
                )
            )
        }
    }
}