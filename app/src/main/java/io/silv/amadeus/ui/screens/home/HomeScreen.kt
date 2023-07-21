package io.silv.amadeus.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import io.silv.amadeus.ui.composables.AnimatedBoxShimmer
import io.silv.amadeus.ui.composables.ConfirmCloseAppPopup
import io.silv.amadeus.ui.composables.HomeTopBar
import io.silv.amadeus.ui.composables.MangaListItem
import io.silv.amadeus.ui.screens.manga_view.MangaViewScreen
import io.silv.amadeus.ui.theme.LocalBottomBarVisibility
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.domain.models.DomainManga
import kotlinx.coroutines.launch

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
        val popularMangaListState = rememberLazyListState()
        var bottomBarVisibility by LocalBottomBarVisibility.current

        LaunchedEffect(Unit) {
            bottomBarVisibility = true
            launch {
                snapshotFlow { gridState.canScrollForward }.collect {
                    if (!it) {
                        sm.loadNextRecentPage()
                    }
                }
            }
            launch {
                snapshotFlow { popularMangaListState.firstVisibleItemIndex }.collect { i ->
                    if (i + 2 >= popularMangaListState.layoutInfo.totalItemsCount * 0.9 && !sm.loadingPopularManga) {
                        sm.loadNextPopularPage()
                    }
                }
            }
        }

        ConfirmCloseAppPopup()

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
                header {
                    Column {
                        Text(text = "Trending", style = MaterialTheme.typography.headlineMedium)
                        LazyRow(
                            state = popularMangaListState,
                        ) {
                            itemsIndexed(popularMangaState) { i, manga ->
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                ) {
                                    Text(
                                        modifier = Modifier
                                            .vertical()
                                            .rotate(-90f)
                                            .padding(space.small)
                                            .offset(x = 60.dp)
                                            .widthIn(0.dp, 240.dp),
                                        text = "${i + 1} ${manga.titleEnglish}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                    )
                                    MangaListItem(
                                        manga = manga,
                                        modifier = Modifier
                                            .padding(space.large)
                                            .width(240.dp)
                                            .height(290.dp)
                                            .clickable {
                                                navigator?.push(
                                                    MangaViewScreen(manga)
                                                )
                                            },
                                        onBookmarkClick = { sm.bookmarkManga(manga.id) }
                                    )
                                }
                            }
                            if (sm.loadingPopularManga) {
                                items(5) {
                                    AnimatedBoxShimmer(
                                        Modifier
                                            .padding(space.large)
                                            .width(240.dp)
                                            .height(290.dp)
                                    )
                                }
                            }
                        }}
                }
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

fun LazyGridScope.header(
    content: @Composable LazyGridItemScope.() -> Unit
) {
    item(span = { GridItemSpan(this.maxLineSpan) }, content = content)
}

fun Modifier.vertical() =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.height, placeable.width) {
            placeable.place(
                x = -(placeable.width / 2 - placeable.height / 2),
                y = -(placeable.height / 2 - placeable.width / 2)
            )
        }
    }