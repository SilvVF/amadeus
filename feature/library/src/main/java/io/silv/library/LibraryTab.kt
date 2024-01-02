@file:OptIn(ExperimentalMaterial3Api::class)

package io.silv.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import io.silv.common.emptyImmutableList
import io.silv.domain.chapter.model.Chapter
import io.silv.domain.manga.model.Manga
import io.silv.domain.manga.model.MangaWithChapters
import io.silv.library.state.LibraryActions
import io.silv.library.state.LibraryError
import io.silv.library.state.LibraryState
import io.silv.ui.LocalAppState
import io.silv.ui.ReselectTab
import io.silv.ui.composables.MangaListItem
import io.silv.ui.composables.SearchTextField
import io.silv.ui.layout.ExpandableInfoLayout
import io.silv.ui.layout.ExpandableState
import io.silv.ui.layout.TopAppBarWithBottomContent
import io.silv.ui.layout.rememberExpandableState
import io.silv.ui.theme.AmadeusTheme
import io.silv.ui.theme.LocalSpacing
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.IgnoredOnParcel

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
                    state.success?.filteredTagIds?.fastForEach(screenModel::onTagFiltered)
                },
                searchOnMangaDex = {
                    appState.searchGlobal(it)
                },
                navigateToExploreTab = {
                    appState.searchGlobal(null)
                }
            )
        )
    }
}

enum class LibTab {
    Library, Chapters, Updates, UserLists {
        override fun toString(): String {
            return "User Lists"
        }
    }
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
                    Text(
                        text = remember(selectedTabProvider()) {
                            selectedTabProvider.invoke().toString()
                        }
                    )
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
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = stateProvider()) {
                LibraryState.Loading -> {}
                is LibraryState.Error -> {
                    ErrorScreenContent(
                        state = state,
                        paddingValues = paddingValues,
                        navigateToExploreTab = actions.navigateToExploreTab
                    )
                }
                is LibraryState.Success -> {
                    SuccessScreenContent(
                        state = state,
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
                        currentTab = currentTab,
                        actions = actions
                    )
                }
            ) {
                Surface(
                    Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                ) {

                }
            }
        }
    }
}

@Composable
fun ErrorScreenContent(
    state: LibraryState.Error,
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
                val libEmpty = "Your library is currently empty. Add manga using the "
                val tab = "Explore Tab"
                addStyle(style = MaterialTheme.typography.labelLarge.toSpanStyle(), 0, libEmpty.length)
                addStyle(
                    style = MaterialTheme.typography.labelLarge
                        .toSpanStyle().copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        ), 
                    libEmpty.length, libEmpty.length + tab.length,
                )
                append(libEmpty)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryPeekContent(
    modifier: Modifier = Modifier,
    stateProvider: () -> LibraryState,
    currentTab: LibTab,
    actions: LibraryActions,
) {

    val space = LocalSpacing.current
    val state = stateProvider()

    LazyRow(
        modifier = modifier
    ) {
        state.success?.libraryTags?.let { tags ->
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

@Composable
fun SuccessScreenContent(
    state: LibraryState.Success,
    paddingValues: PaddingValues,
    currentTab: LibTab,
    actions: LibraryActions,
) {
    val space = LocalSpacing.current

    val showGlobalSearch by remember(state) {
        derivedStateOf {
            state.filteredTagIds.isEmpty() &&
            state.filteredMangaWithChapters.isEmpty() &&
            state.filteredText.isNotBlank()
        }
    }


    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
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
            key = { (manga, _) -> manga.id }
        ) { (manga, _) ->
            MangaListItem(
                manga = manga,
                modifier = Modifier
                    .padding(space.small)
                    .aspectRatio(2f / 3f),
                onTagClick = {},
                onBookmarkClick = {}
            )
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
        LibraryState.Error(LibraryError.NoFavoritedChapters)
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
        LibraryState.Error(LibraryError.NoFavoritedChapters)
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
            LibraryState.Success(
                filteredMangaWithChapters = buildList {
                    repeat(5) {
                        val manga = Manga.stub()
                        add(
                            MangaWithChapters(
                                manga = manga,
                                chapters = buildList {
                                    repeat(30) {
                                        add(Chapter.stub(manga.id, 1, it.toLong()))
                                    }
                                }
                                    .toImmutableList()
                            )
                        )
                    }
                }.toImmutableList()
            )
        )
    }
    AmadeusTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            LibraryScreenContent(
                stateProvider = { state },
                searchTextProvider = { state.filteredText },
                actions = LibraryActions(
                    filterByTag = {
                        state = state.copy(
                            filteredTagIds = (if (state.filteredTagIds.contains(it))
                                state.filteredTagIds - it
                            else
                                state.filteredTagIds + it
                        ).toImmutableList())
                    },
                    clearTagFilter = {
                        state = state.copy(
                            filteredTagIds = persistentListOf()
                        )
                    },
                    searchChanged = { t ->
                        state = state.copy(
                            filteredText = t,
                            mangaWithChapters = emptyImmutableList()
                        )
                    }
                )
            )
        }
    }
}