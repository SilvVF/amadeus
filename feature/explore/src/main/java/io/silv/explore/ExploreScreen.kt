package io.silv.explore

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.More
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.silv.datastore.asState
import io.silv.datastore.model.ExplorePrefs
import io.silv.explore.composables.DisplayOptionsBottomSheet
import io.silv.explore.composables.ExploreTopAppBar
import io.silv.explore.composables.FiltersBottomSheet
import io.silv.explore.composables.SeasonalMangaPager
import io.silv.explore.composables.mangaGrid
import io.silv.model.SavableManga
import io.silv.navigation.SharedScreen
import io.silv.navigation.push
import io.silv.ui.CenterBox
import io.silv.ui.composables.AnimatedBoxShimmer
import io.silv.ui.composables.CardType
import io.silv.ui.composables.MangaListItem
import io.silv.ui.composables.PullRefresh
import io.silv.ui.layout.ExpandableInfoLayout
import io.silv.ui.layout.rememberExpandableState
import io.silv.ui.theme.LocalSpacing
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch


class ExploreScreen: Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {

        val screenModel = getScreenModel<ExploreScreenModel>()

        val pagingFlowFlow by screenModel.mangaPagingFlow.collectAsStateWithLifecycle()
        val state by screenModel.state.collectAsStateWithLifecycle()

        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(state = rememberTopAppBarState())

        val expandableState = rememberExpandableState(startProgress = SheetValue.Hidden)
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(ExploreTab.reselectChannel) {
            ExploreTab.reselectChannel.receiveAsFlow().collect {
                Log.d("Explore", "received reselect event")
                expandableState.toggleProgress()
            }
        }

        var showDisplayOptionsBottomSheet by rememberSaveable {
            mutableStateOf(false)
        }

        var showFiltersBottomSheet by rememberSaveable {
            mutableStateOf(false)
        }
        val changePageType = screenModel::changePagingType
        when {
            showDisplayOptionsBottomSheet -> DisplayOptionsBottomSheet(
                optionsTitle = {
                    Text("Explore display options",)
                },
                onDismissRequest = { showDisplayOptionsBottomSheet = false },
                clearSearchHistory = screenModel::clearSearchHistory
            )
            showFiltersBottomSheet -> FiltersBottomSheet(
                onSaveQuery = {
                   changePageType(UiPagedType.Query(it))
                }
            ) {
                showFiltersBottomSheet = !showFiltersBottomSheet
            }
        }

        Scaffold(
            topBar = {
                val scope = rememberCoroutineScope()

                ExploreTopAppBar(
                    selected = state.pagedType,
                    scrollBehavior = scrollBehavior,
                    onWebClick = {  },
                    onDisplayOptionsClick = {
                        scope.launch {
                            if (expandableState.isExpanded)
                                expandableState.hide()
                            else
                                expandableState.expand()
                        }
                    },
                    onSearch = screenModel::onSearch,
                    onPageTypeSelected = screenModel::changePagingType,
                    onFilterClick = {
                        showFiltersBottomSheet = !showFiltersBottomSheet
                    },
                )
            },
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { paddingValues ->

            val pagingItems = pagingFlowFlow.collectAsLazyPagingItems()

            Box(Modifier.fillMaxSize()) {
                PullRefresh(
                    paddingValues = paddingValues,
                    refreshing = false, // refresh indicator handled by BrowseMangaContent
                    onRefresh = when (state.pagedType) {
                        UiPagedType.Seasonal -> screenModel::refreshSeasonalManga
                        else -> pagingItems::refresh
                    }
                ) {
                    BrowseMangaContent(
                        modifier = Modifier.fillMaxSize(),
                        contentPaddingValues = paddingValues,
                        seasonalLists = state.seasonalLists,
                        refreshingSeasonal = state.refreshingSeasonal,
                        mangaList = pagingItems,
                        onBookmarkClick = screenModel::bookmarkManga,
                        onMangaClick = {
                            navigator.push(
                                SharedScreen.MangaView(it.id)
                            )
                        },
                        onTagClick = { name, id ->
                            navigator.push(
                                SharedScreen.MangaFilter(name, id)
                            )
                        },
                        pagedType = state.pagedType,
                    )
                }
                ExpandableInfoLayout(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    state = expandableState,
                    peekContent = {
                        RecentSearchesPeekContent(
                            modifier = Modifier.fillMaxWidth(),
                            recentSearchUiState = state.recentSearchUiState,
                            query = state.filters?.title,
                            onRecentSearchClick = screenModel::onSearch
                        )
                    }
                ) {
                    ExpandableInfoLayoutContent(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        showGroupingOptions = {
                            showFiltersBottomSheet = !showFiltersBottomSheet
                        },
                        showDisplayOptions = {
                            showDisplayOptionsBottomSheet = !showDisplayOptionsBottomSheet
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentSearchesPeekContent(
    modifier: Modifier = Modifier,
    query: String?,
    recentSearchUiState: RecentSearchUiState,
    onRecentSearchClick: (String) -> Unit,
) {
    val space = LocalSpacing.current
    when (recentSearchUiState) {
        RecentSearchUiState.Loading -> {
            Row(modifier) {
                repeat(5) {
                    AnimatedBoxShimmer(
                        Modifier
                            .height(SuggestionChipDefaults.Height + 8.dp)
                            .weight(1f)
                            .padding(horizontal = space.small)
                    )
                }
            }
        }
        is RecentSearchUiState.Success -> {
            LazyRow(
                modifier = modifier
            ) {
                item(
                    key = "padding-start"
                ) {
                    Spacer(modifier = Modifier.padding(space.small))
                }
                if (recentSearchUiState.recentQueries.isEmpty()) {
                    items(
                        items = persistentListOf(
                            "Oshi no ko", "Steins Gate", "Dr Stone", "Bleach"
                        ),
                        key = { item -> item }
                    ) { suggestedSearch ->
                        ElevatedFilterChip(
                            selected = suggestedSearch == query,
                            modifier = Modifier.padding(space.small),
                            onClick = { onRecentSearchClick(suggestedSearch) },
                            label = { Text(text = suggestedSearch) }
                        )
                    }
                } else {
                    items(
                        recentSearchUiState.recentQueries,
                        key = { item -> item.query }
                    ) { recentSearch ->
                        ElevatedFilterChip(
                            selected = recentSearch.query == query,
                            modifier = Modifier.padding(space.small),
                            onClick = { onRecentSearchClick(recentSearch.query) },
                            label = { Text(text = recentSearch.query) }
                        )
                    }
                }
                item(
                    key = "padding-end"
                ) {
                    Spacer(modifier = Modifier.padding(space.small))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandableInfoLayoutContent(
    modifier: Modifier = Modifier,
    showGroupingOptions: () -> Unit,
    showDisplayOptions: () -> Unit
) {
    val space = LocalSpacing.current
    val surfaceColor =  MaterialTheme.colorScheme.surfaceColorAtElevation(BottomSheetDefaults.Elevation)

    Column(
        modifier
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .drawBehind {
                drawRect(color = surfaceColor)
            }
            .padding(space.small)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { showGroupingOptions() }
                .padding(space.med),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.More,
                contentDescription = "Filter",
                modifier = Modifier.graphicsLayer { rotationX = 180f }
            )
            Spacer(modifier = Modifier.width(space.med))
            Text(text = "Filter manga by...")
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { showDisplayOptions() }
                .padding(space.med),
            verticalAlignment = Alignment.CenterVertically
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


@Composable
fun BrowseMangaContent(
    modifier: Modifier,
    contentPaddingValues: PaddingValues,
    pagedType: UiPagedType,
    gridState: LazyGridState = rememberLazyGridState(),
    seasonalLists: ImmutableList<UiSeasonalList>,
    refreshingSeasonal: Boolean,
    mangaList: LazyPagingItems<SavableManga>,
    onMangaClick: (manga: SavableManga) -> Unit,
    onTagClick: (name: String, id: String) -> Unit,
    onBookmarkClick: (mangaId: String) -> Unit
) {
    val navigator = LocalNavigator.current

    val gridCells by ExplorePrefs.gridCellsPrefKey.asState(ExplorePrefs.gridCellsDefault)
    val showSeasonalLists by ExplorePrefs.showSeasonalListPrefKey.asState(ExplorePrefs.showSeasonalDefault)
    val cardType by ExplorePrefs.cardTypePrefKey.asState(
        defaultValue = CardType.Compact,
        store = { it.toString() },
        convert = { CardType.valueOf(it) }
    )
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    val refreshing = pagedType is UiPagedType.Seasonal && refreshingSeasonal || mangaList.loadState.refresh is LoadState.Loading

    when {
        refreshing -> CenterBox(Modifier.fillMaxSize()) {
            CircularProgressIndicator()
        }
        mangaList.loadState.refresh is LoadState.Error -> {
            CenterBox(Modifier.fillMaxSize()) {
                Button(onClick = { mangaList.retry() }) {
                    Text("Retry loading manga.")
                }
            }
        }
        else -> {
            LazyVerticalGrid(
                modifier = modifier.fillMaxSize(),
                state = gridState,
                columns = GridCells.Fixed(gridCells),
                contentPadding = contentPaddingValues
            ) {
                if (showSeasonalLists && pagedType !is UiPagedType.Seasonal) {
                    seasonalMangaPagerGridItem(
                        gridCells = gridCells,
                        refreshing = refreshingSeasonal,
                        seasonalLists = seasonalLists,
                        onMangaClick = onMangaClick,
                        onTagClick = onTagClick,
                        onBookmarkClick = {
                            onBookmarkClick(it.id)
                        }
                    )
                }
                if (pagedType is UiPagedType.Seasonal) {
                    item(
                        key = "seasonal-tags",
                        span = { GridItemSpan(gridCells) }
                    ) {
                        SeasonalListTags(
                            seasonalLists = seasonalLists,
                            refreshing = refreshingSeasonal,
                            selectedIndex = selectedIndex,
                            onIndexSelected = {
                                selectedIndex = it
                            }
                        )
                    }
                    seasonalMangaGrid(
                        cardType,
                        seasonalLists.getOrNull(selectedIndex),
                        onMangaClick = onMangaClick,
                        onBookmarkClick = {
                            onBookmarkClick(it.id)
                        },
                        onTagClick = onTagClick
                    )
                } else {
                    mangaGrid(
                        manga = mangaList,
                        cardType = cardType,
                        onBookmarkClick = { manga ->
                            onBookmarkClick(manga.id)
                        },
                        onTagClick = { manga, name ->
                            manga.tagToId[name]?.let { id ->
                                navigator?.push(
                                    SharedScreen.MangaFilter(name, id)
                                )
                            }
                        },
                        onMangaClick = { manga ->
                            navigator?.push(
                                SharedScreen.MangaView(manga.id)
                            )
                        },
                    )
                }
            }
        }
    }
}

private fun LazyGridScope.seasonalMangaGrid(
    cardType: CardType,
    seasonalList: UiSeasonalList?,
    onMangaClick: (manga: SavableManga) -> Unit,
    onBookmarkClick: (manga: SavableManga) -> Unit,
    onTagClick: (name: String, id: String) -> Unit,
) {
    seasonalList?.let {
        items(
            items = seasonalList.mangas,
            key = { v -> v.value.id }
        ) { item ->

            val space = LocalSpacing.current
            val manga by item.collectAsState()

            MangaListItem(
                manga = manga,
                cardType = cardType,
                modifier = Modifier
                    .padding(space.small)
                    .aspectRatio(2f / 3f)
                    .clickable {
                        onMangaClick(manga)
                    },
                onTagClick = { name ->
                    manga.tagToId[name]?.let { onTagClick(name, it) }
                },
                onBookmarkClick = {
                    onBookmarkClick(manga)
                }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
private fun LazyGridScope.seasonalMangaPagerGridItem(
    gridCells: Int,
    refreshing: Boolean,
    seasonalLists: ImmutableList<UiSeasonalList>,
    onMangaClick: (manga: SavableManga) -> Unit,
    onBookmarkClick: (manga: SavableManga) -> Unit,
    onTagClick: (name: String, id: String) -> Unit,
) {
    item(
        key = "seasonal-tag",
        span = { GridItemSpan(gridCells) }
    ) {
        var selectedIndex by rememberSaveable {
            mutableIntStateOf(0)
        }
        SeasonalListTags(
            seasonalLists = seasonalLists,
            refreshing = refreshing,
            selectedIndex = selectedIndex,
            onIndexSelected = {
                selectedIndex = it
            }
        )
        if (refreshing) {
            AnimatedBoxShimmer(
                Modifier
                    .fillMaxWidth()
                    .height(240.dp))
        } else {
            SeasonalMangaPager(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                mangaList = seasonalLists.getOrNull(selectedIndex)?.mangas ?: persistentListOf(),
                onMangaClick = onMangaClick,
                onBookmarkClick = onBookmarkClick,
                onTagClick = onTagClick
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonalListTags(
    modifier: Modifier = Modifier,
    seasonalLists: ImmutableList<UiSeasonalList>,
    refreshing: Boolean,
    selectedIndex: Int,
    onIndexSelected: (Int) -> Unit
) {
    val space = LocalSpacing.current
    Column(modifier) {
        Text(
            text = "seasonal lists",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(space.small)
        )
        LazyRow {
            if (refreshing) {
                items(4, key = { "refreshing-tag$it" }) {
                    Row {
                        AnimatedBoxShimmer(
                            Modifier
                                .width(90.dp)
                                .height(FilterChipDefaults.Height)
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = seasonalLists,
                    key = { _, list -> list.id }
                ) { index, seasonalList ->
                    FilterChip(
                        selected = index == selectedIndex,
                        onClick = { onIndexSelected(index) },
                        label = {
                            val text = remember(seasonalList) {
                                "${seasonalList.season.name}  ${
                                    seasonalList.year.toString().takeLast(2)
                                }"
                            }
                            Text(
                                text = text,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(space.xs)
                    )
                }
            }
        }
    }
}



