package io.silv.amadeus.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import io.silv.amadeus.ui.composables.AnimatedBoxShimmer
import io.silv.amadeus.ui.composables.HomeTopBar
import io.silv.amadeus.ui.composables.MangaListItem
import io.silv.amadeus.ui.screens.manga_view.MangaViewScreen
import io.silv.amadeus.ui.theme.LocalBottomBarVisibility
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.domain.models.DomainManga

class HomeScreen: Screen {

    @Composable
    override fun Content() {
        val sm = getScreenModel<HomeSM>()

        val recentMangaState by sm.recentMangaUiState.collectAsStateWithLifecycle()
        val popularMangaState by sm.popularMangaUiState.collectAsStateWithLifecycle()
        val searchText by sm.searchText.collectAsStateWithLifecycle()

        val space = LocalSpacing.current
        val navigator = LocalNavigator.current
        val gridState = rememberLazyGridState()
        var bottomBarVisibility by LocalBottomBarVisibility.current

        LaunchedEffect(Unit) {
            bottomBarVisibility = true
            snapshotFlow { gridState.canScrollForward }.collect {
                if (!it) {
                    sm.loadNextRecentPage()
                }
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .navigationBarsPadding()
        ) {
            HomeTopBar(
                modifier = Modifier.fillMaxWidth(),
                searchText = searchText,
                onSearchTextChange = { query ->
                    sm.searchTextChanged(query)
                }
            )
            LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                state = gridState,
                columns = GridCells.Fixed(2)
            ) {
                items(
                    items = recentMangaState,
                    key = {item: DomainManga -> item.id }
                ) { manga ->
                    MangaListItem(
                        manga = manga,
                        modifier = Modifier
                            .padding(space.large)
                            .clickable {
                                navigator?.push(
                                    MangaViewScreen(manga)
                                )
                            },
                        onBookmarkClick = { sm.bookmarkManga(manga.id) }
                    )
                }
            }
            if (sm.loadingRecentManga) {
                Row(Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(2) {
                        AnimatedBoxShimmer(Modifier.size(220.dp))
                    }
                }
            }
        }
    }
}