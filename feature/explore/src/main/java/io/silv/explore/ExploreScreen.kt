package io.silv.explore

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import coil.compose.AsyncImage
import io.silv.explore.composables.SearchItemsPagingList
import io.silv.explore.composables.TrendingMangaList
import io.silv.explore.composables.recentMangaList
import io.silv.model.SavableManga
import io.silv.navigation.SharedScreen
import io.silv.navigation.push
import io.silv.ui.BlurImageBackground
import io.silv.ui.CenterBox
import io.silv.ui.MangaGenreTags
import io.silv.ui.PullRefresh
import io.silv.ui.SearchTopAppBar
import io.silv.ui.TranslatedLanguageTags
import io.silv.ui.theme.LocalSpacing
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class ExploreScreen: Screen {

    @OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {

        val sm = getScreenModel<ExploreScreenModel>()

        val recentPagingFlowFlow by sm.recentMangaPagingFlow.collectAsStateWithLifecycle()
        val popularPagingFlowFlow by sm.popularMangaPagingFlow.collectAsStateWithLifecycle()
        val searchPagingFlowFlow by sm.searchMangaPagingFlow.collectAsStateWithLifecycle()
        val seasonalLists by sm.seasonLists.collectAsStateWithLifecycle()

        val state by sm.state.collectAsStateWithLifecycle()
        val navigator = LocalNavigator.current

        var searching by rememberSaveable {
            mutableStateOf(false)
        }

        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(state = rememberTopAppBarState())


        Scaffold(
            topBar = {
                SearchTopAppBar(
                    scrollBehavior = scrollBehavior,
                    onSearchText = sm::updateSearchQuery,
                    color = Color.Transparent,
                    navigationIconLabel = "",
                    navigationIcon = Icons.Filled.KeyboardArrowLeft,
                    onNavigationIconClicked = { searching = false },
                    actions = {},
                    searchText = state.searchQuery,
                    showTextField = searching,
                    onSearchChanged = {
                        searching = it
                    },
                    onForceSearch = {
                        sm.startSearching()
                    }
                )
            },
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { paddingValues ->
            Column(Modifier.padding(paddingValues)) {
                AnimatedContent(
                    targetState = searching,
                    label = "searching"
                ) {
                    if (it) {
                        SearchItemsPagingList(
                            modifier = Modifier.fillMaxSize(),
                            items = searchPagingFlowFlow.collectAsLazyPagingItems(),
                            onMangaClick = { manga ->
                                navigator?.push(
                                    SharedScreen.MangaView(manga.id)
                                )
                            },
                            onBookmarkClick = { manga ->
                                sm.bookmarkManga(manga.id)
                            },
                        )
                    } else {

                        val recentMangaItems = recentPagingFlowFlow.collectAsLazyPagingItems()
                        val popularMangaItems = popularPagingFlowFlow.collectAsLazyPagingItems()

                        PullRefresh(
                            refreshing = recentMangaItems.loadState.refresh is LoadState.Loading
                                    && popularMangaItems.loadState.refresh == LoadState.Loading,
                            onRefresh = {
                                recentMangaItems.refresh()
                                popularMangaItems.refresh()
                                sm.refreshSeasonalManga()
                            }
                        ) {
                            BrowseMangaContent(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                recentMangaList = recentMangaItems,
                                onBookmarkClick = sm::bookmarkManga,
                                popularMangaList = popularMangaItems,
                                seasonalLists = seasonalLists,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseMangaContent(
    modifier: Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
    seasonalLists: ImmutableList<UiSeasonalList>,
    recentMangaList: LazyPagingItems<SavableManga>,
    popularMangaList: LazyPagingItems<SavableManga>,
    onBookmarkClick: (mangaId: String) -> Unit
) {
    val space = LocalSpacing.current
    val navigator = LocalNavigator.current

    LazyVerticalGrid(
        modifier = modifier,
        state = gridState,
        columns = GridCells.Fixed(2)
    ) {
        item(
            key = "seasonal-tag",
            span = { GridItemSpan(2) }
        ) {
            var selectedIndex by rememberSaveable {
                mutableIntStateOf(0)
            }

            Column(Modifier.padding(space.med)) {
                Text(
                    "seasonal lists",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(space.xs)
                )
                LazyRow {
                    itemsIndexed(
                        items = seasonalLists,
                        key = { _, list -> list.id }
                    ) { index, seasonalList ->
                        FilterChip(
                            selected = index == selectedIndex,
                            onClick = { selectedIndex = index },
                            label = {
                                Text("${seasonalList.season.name}  ${seasonalList.year.toString().takeLast(2)}",
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(space.xs)
                        )
                    }
                }
                SeasonalMangaPager(
                    mangaList = seasonalLists.getOrNull(selectedIndex)?.mangas ?: persistentListOf(),
                    onMangaClick = {},
                    onBookmarkClick = {},
                    onTagClick = { name, id -> }
                )
            }
        }
        item(
            key = "trending-tag",
            span = { GridItemSpan(2) }
        ) {
            Text(
                text = "Trending",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(space.med)
            )
        }
        item(
            key = "trending-manga-list",
            span = { GridItemSpan(2) }
        ) {
            TrendingMangaList(
                manga = popularMangaList,
                onBookmarkClick = {
                    onBookmarkClick(it.id)
                }
            )
        }
        item(
            key = "recently-update-tag",
            span = { GridItemSpan(2) }
        ) {
            Text(
                text = "Recently Updated",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(space.med)
            )
        }
        recentMangaList(
            manga = recentMangaList,
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


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SeasonalMangaPager(
    mangaList: ImmutableList<StateFlow<SavableManga>>,
    onMangaClick: (manga: SavableManga) -> Unit,
    onBookmarkClick: (manga: SavableManga) -> Unit,
    onTagClick: (name: String, id: String) -> Unit,
) {
    val space = LocalSpacing.current

    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f,
        pageCount = {
            mangaList.size
        }
    )
    val scope = rememberCoroutineScope()

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .height(240.dp)
            .fillMaxWidth()
    ) { page ->

        val manga by mangaList[page].collectAsStateWithLifecycle()

        BlurImageBackground(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    onMangaClick(manga)
                },
            url = manga.coverArt
        ) {

            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .padding(space.med),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.Start
                ) {
                    CenterBox(Modifier.height(230.dp)) {
                        AsyncImage(
                            model = manga,
                            contentDescription = null,
                            contentScale = ContentScale.Inside,
                            modifier = Modifier
                                .fillMaxWidth(0.4f)
                        )
                    }
                    Spacer(modifier = Modifier.width(space.med))
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = manga.titleEnglish,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            maxLines = 2,
                            fontSize = 20.sp,
                            overflow = TextOverflow.Ellipsis
                        )
                        TranslatedLanguageTags(
                            tags = manga.availableTranslatedLanguages,
                            modifier = Modifier.fillMaxWidth()
                        )
                        MangaGenreTags(
                            tags = manga.tagToId.keys.toList(),
                            modifier = Modifier.fillMaxWidth(),
                            onTagClick = { name ->
                                manga.tagToId[name]?.let {
                                    onTagClick(name, it)
                                }
                            }
                        )
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(onClick = { onBookmarkClick(manga) }) {
                                    Icon(
                                        imageVector = if (manga.bookmarked)
                                            Icons.Filled.Favorite
                                        else
                                            Icons.Outlined.FavoriteBorder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                Text(
                                    text = if (manga.bookmarked)
                                        "In library"
                                    else
                                        "Add to library",
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "NO.${page + 1}",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                )
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            pagerState.animateScrollToPage(page - 1)
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowLeft,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            pagerState.animateScrollToPage(page + 1)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}









