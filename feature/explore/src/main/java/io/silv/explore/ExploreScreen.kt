package io.silv.explore

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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

        val recentPagingFlowFlow by sm.recentMangaPagingFlow.collectAsStateWithLifecycle()
        val popularPagingFlowFlow by sm.popularMangaPagingFlow.collectAsStateWithLifecycle()
        val searchPagingFlowFlow by sm.searchMangaPagingFlow.collectAsStateWithLifecycle()

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
                                    SharedScreen.MangaView(manga)
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
    gridState: LazyGridState = rememberLazyGridState(),
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
                    SharedScreen.MangaView(manga)
                )
            },
        )
    }
}









