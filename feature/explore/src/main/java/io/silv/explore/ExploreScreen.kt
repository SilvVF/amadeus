package io.silv.explore

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import io.silv.explore.composables.SearchItemsPagingList
import io.silv.explore.composables.TrendingMangaList
import io.silv.explore.composables.recentMangaList
import io.silv.explore.composables.seasonalMangaLists
import io.silv.model.SavableManga
import io.silv.navigation.SharedScreen
import io.silv.navigation.push
import io.silv.ui.PullRefresh
import io.silv.ui.SearchTopAppBar
import io.silv.ui.theme.LocalSpacing


class ExploreScreen: Screen {

    @OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {

        val sm = getScreenModel<ExploreScreenModel>()

        val recentMangaItems = sm.recentMangaPagingFlow.collectAsLazyPagingItems()
        val popularMangaItems = sm.popularMangaPagingFlow.collectAsLazyPagingItems()
        val searchMangaState = sm.searchMangaPagingFlow.collectAsLazyPagingItems()
        val seasonalMangaState by sm.seasonalMangaUiState.collectAsStateWithLifecycle()
        val refreshingSeasonal by sm.refreshingSeasonal.collectAsStateWithLifecycle()
        val searchQuery by sm.searchQuery.collectAsStateWithLifecycle()
        val navigator = LocalNavigator.current
        val recentListState = rememberLazyListState()

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
                    searchText = searchQuery,
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
                            items = searchMangaState,
                            onMangaClick = { manga ->
                                navigator?.push(
                                    SharedScreen.MangaView(manga)
                                )
                            },
                            onBookmarkClick = { manga ->
                                sm.bookmarkManga(manga.id)
                            },
                        )
                    } else {
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
                                recentMangaLazyListState = recentListState,
                                recentMangaList = recentMangaItems,
                                seasonalMangaList = seasonalMangaState,
                                seasonalRefreshing = refreshingSeasonal,
                                onBookmarkClick = sm::bookmarkManga,
                                popularMangaList = popularMangaItems
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BrowseMangaContent(
    modifier: Modifier,
    recentMangaLazyListState: LazyListState,
    recentMangaList: LazyPagingItems<SavableManga>,
    seasonalMangaList: SeasonalMangaUiState,
    seasonalRefreshing: Boolean,
    popularMangaList: LazyPagingItems<SavableManga>,
    onBookmarkClick: (mangaId: String) -> Unit
) {
    val space = LocalSpacing.current
    val navigator = LocalNavigator.current

    LazyColumn(
        modifier = modifier,
        state = recentMangaLazyListState,
    ) {
        seasonalMangaLists(
            refreshingSeasonal = seasonalRefreshing,
            seasonalMangaState = seasonalMangaList,
            onBookmarkClick = {
                onBookmarkClick(it.id)
            }
        )
        item(
            key = "trending-tag"
        ) {
            Text(
                text = "Trending",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(space.med)
            )
        }
        item(
            key = "trending-manga-list"
        ) {
            TrendingMangaList(
                manga = popularMangaList,
                onBookmarkClick = {
                    onBookmarkClick(it.id)
                }
            )
        }
        item(
            key = "recently-update-tag"
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
                    SharedScreen.MangaView(manga)
                )
            },
        )
    }
}









