package io.silv.explore

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.material.icons.automirrored.outlined.More
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.silv.datastore.ExplorePrefs
import io.silv.datastore.collectAsState
import io.silv.domain.manga.model.Manga
import io.silv.model.DomainSeasonalList
import io.silv.navigation.SharedScreen
import io.silv.navigation.push
import io.silv.ui.CenterBox
import io.silv.ui.Converters
import io.silv.ui.composables.AnimatedBoxShimmer
import io.silv.ui.composables.CardType
import io.silv.ui.composables.MangaGridItem
import io.silv.ui.composables.MangaListItem
import io.silv.ui.composables.mangaGrid
import io.silv.ui.composables.mangaList
import io.silv.ui.theme.LocalSpacing


import kotlinx.coroutines.flow.StateFlow

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
                            .padding(horizontal = space.small),
                    )
                }
            }
        }
        is RecentSearchUiState.Success -> {
            LazyRow(
                modifier = modifier,
            ) {
                item(
                    key = "padding-start",
                ) {
                    Spacer(modifier = Modifier.padding(space.small))
                }
                if (recentSearchUiState.recentQueries.isEmpty()) {
                    items(
                        items =
                        listOf(
                            "Oshi no ko",
                            "Steins Gate",
                            "Dr Stone",
                            "Bleach",
                        ),
                        key = { item -> item },
                    ) { suggestedSearch ->
                        ElevatedFilterChip(
                            selected = suggestedSearch == query,
                            modifier = Modifier.padding(space.small),
                            onClick = { onRecentSearchClick(suggestedSearch) },
                            label = { Text(text = suggestedSearch) },
                        )
                    }
                } else {
                    items(
                        recentSearchUiState.recentQueries,
                        key = { item -> item.query },
                    ) { recentSearch ->
                        ElevatedFilterChip(
                            selected = recentSearch.query == query,
                            modifier = Modifier.padding(space.small),
                            onClick = { onRecentSearchClick(recentSearch.query) },
                            label = { Text(text = recentSearch.query) },
                        )
                    }
                }
                item(
                    key = "padding-end",
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
    showDisplayOptions: () -> Unit,
) {
    val space = LocalSpacing.current
    val surfaceColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
        BottomSheetDefaults.Elevation
    )

    Column(
        modifier
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .drawBehind {
                drawRect(color = surfaceColor)
            }
            .padding(space.small),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { showGroupingOptions() }
                .padding(space.med),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.More,
                contentDescription = "Filter",
                modifier = Modifier.graphicsLayer { rotationX = 180f },
            )
            Spacer(modifier = Modifier.width(space.med))
            Text(text = "Filter manga by...")
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { showDisplayOptions() }
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

@Composable
fun BrowseMangaContent(
    modifier: Modifier,
    contentPaddingValues: PaddingValues,
    pagedType: UiPagedType,
    gridState: LazyGridState = rememberLazyGridState(),
    seasonalLists: List<DomainSeasonalList>,
    refreshingSeasonal: Boolean,
    mangaList: LazyPagingItems<StateFlow<Manga>>,
    onMangaClick: (manga: Manga) -> Unit,
    onTagClick: (name: String, id: String) -> Unit,
    onBookmarkClick: (mangaId: String) -> Unit,
) {
    val navigator = LocalNavigator.currentOrThrow
    val scope = rememberCoroutineScope()

    val gridCells by ExplorePrefs.gridCellsPrefKey.collectAsState(ExplorePrefs.gridCellsDefault, scope)
    val cardType by ExplorePrefs.cardTypePrefKey.collectAsState(
        defaultValue = CardType.Compact,
        converter = Converters.CardTypeToStringConverter,
        scope = scope
    )
    val useList by ExplorePrefs.useListPrefKey.collectAsState(false, scope)
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    val refreshing = if (pagedType is UiPagedType.Seasonal) {
        refreshingSeasonal
    } else {
        (mangaList.loadState.refresh is LoadState.Loading)
    }

    when {
        refreshing ->
            CenterBox(Modifier.fillMaxSize()) {
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
            if (useList) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPaddingValues,
                ) {
                    if (pagedType is UiPagedType.Seasonal) {
                        item(
                            key = "seasonal-tags",
                        ) {
                            SeasonalListTags(
                                seasonalLists = seasonalLists,
                                refreshing = refreshingSeasonal,
                                selectedIndex = selectedIndex,
                                onIndexSelected = {
                                    selectedIndex = it
                                },
                            )
                        }
                        seasonalMangaList(
                            seasonalList = seasonalLists.getOrNull(selectedIndex),
                            onMangaClick = onMangaClick,
                            onBookmarkClick = { manga ->
                                onBookmarkClick(manga.id)
                            },
                        )
                    } else {
                        mangaList(
                            manga = mangaList,
                            onFavoriteClick = { manga ->
                                onBookmarkClick(manga.id)
                            },
                            onMangaClick = { manga ->
                                navigator.push(
                                    SharedScreen.MangaView(manga.id),
                                )
                            }
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    modifier = modifier.fillMaxSize(),
                    state = gridState,
                    columns = GridCells.Fixed(gridCells),
                    contentPadding = contentPaddingValues,
                ) {
                    if (pagedType is UiPagedType.Seasonal) {
                        item(
                            key = "seasonal-tags",
                            span = { GridItemSpan(gridCells) },
                        ) {
                            SeasonalListTags(
                                seasonalLists = seasonalLists,
                                refreshing = refreshingSeasonal,
                                selectedIndex = selectedIndex,
                                onIndexSelected = {
                                    selectedIndex = it
                                },
                            )
                        }
                        seasonalMangaGrid(
                            cardType,
                            seasonalLists.getOrNull(selectedIndex),
                            onMangaClick = onMangaClick,
                            onBookmarkClick = {
                                onBookmarkClick(it.id)
                            },
                            onTagClick = onTagClick,
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
                                    navigator.push(
                                        SharedScreen.MangaFilter(name, id),
                                    )
                                }
                            },
                            onMangaClick = { manga ->
                                navigator.push(
                                    SharedScreen.MangaView(manga.id),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.seasonalMangaList(
    seasonalList: DomainSeasonalList?,
    onMangaClick: (manga: Manga) -> Unit,
    onBookmarkClick: (manga: Manga) -> Unit,
) {
    seasonalList?.let {
        items(
            items = seasonalList.mangas,
            key = { v -> v.id },
        ) { manga ->

            val space = LocalSpacing.current

            MangaListItem(
                manga = manga,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(space.small)
                    .clickable {
                        onMangaClick(manga)
                    },
                onFavoriteClick = {
                    onBookmarkClick(manga)
                },
            )
        }
    }
}


private fun LazyGridScope.seasonalMangaGrid(
    cardType: CardType,
    seasonalList: DomainSeasonalList?,
    onMangaClick: (manga: Manga) -> Unit,
    onBookmarkClick: (manga: Manga) -> Unit,
    onTagClick: (name: String, id: String) -> Unit,
) {
    seasonalList?.let {
        items(
            items = seasonalList.mangas,
            key = { v -> v.id },
        ) { manga ->

            val space = LocalSpacing.current

            MangaGridItem(
                manga = manga,
                cardType = cardType,
                modifier =
                Modifier
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
                },
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonalListTags(
    modifier: Modifier = Modifier,
    seasonalLists: List<DomainSeasonalList>,
    refreshing: Boolean,
    selectedIndex: Int,
    onIndexSelected: (Int) -> Unit,
) {
    val space = LocalSpacing.current
    Column(modifier) {
        Text(
            text = "seasonal lists",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(space.small),
        )
        LazyRow {
            if (refreshing) {
                items(4, key = { "refreshing-tag$it" }) {
                    Row {
                        AnimatedBoxShimmer(
                            Modifier
                                .width(90.dp)
                                .height(FilterChipDefaults.Height),
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = seasonalLists,
                    key = { _, list -> list.id },
                ) { index, seasonalList ->
                    FilterChip(
                        selected = index == selectedIndex,
                        onClick = { onIndexSelected(index) },
                        label = {
                            val text =
                                remember(seasonalList) {
                                    "${seasonalList.season.name}  ${
                                    seasonalList.year.toString().takeLast(2)
                                    }"
                                }
                            Text(
                                text = text,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center,
                            )
                        },
                        modifier =
                        Modifier
                            .weight(1f)
                            .padding(space.xs),
                    )
                }
            }
        }
    }
}
