package io.silv.amadeus.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import io.silv.amadeus.ui.theme.LocalPaddingValues
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.domain.models.DomainManga
import kotlinx.coroutines.launch

class HomeScreen: Screen {

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Content() {
        val sm = getScreenModel<HomeSM>()

        val recentMangaState by sm.recentMangaUiState.collectAsStateWithLifecycle()
        val popularMangaState by sm.popularMangaUiState.collectAsStateWithLifecycle()
        val seasonalMangaState by sm.seasonalMangaUiState.collectAsStateWithLifecycle()
        val searchMangaState by sm.searchMangaUiState.collectAsStateWithLifecycle()
        val searchText by sm.searchText.collectAsStateWithLifecycle()
        var searching by rememberSaveable { mutableStateOf(false) }

        val space = LocalSpacing.current
        val navigator = LocalNavigator.current
        val bottomBarPadding by LocalPaddingValues.current
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
                snapshotFlow { popularMangaListState.canScrollForward }.collect {
                    if (!it) {
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
            if (searching) {
                SearchMangaTopBar(
                    searchText = searchText,
                    onSearchTextValueChange = {
                        sm.searchTextChanged(it)
                    },
                    onBackArrowClicked = {
                        searching = false
                    }
                )
                SearchItemsList(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottomBarPadding),
                    items = searchMangaState,
                    onMangaClick = {
                        navigator?.push(
                            MangaViewScreen(it)
                        )
                    },
                    onBookmarkClick = {
                        sm.bookmarkManga(it.id)
                    }
                )
                return
            }
            HomeTopBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(space.large),
                searchButtonClick = {
                    searching = true
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
                        Text(
                            text = "Seasonal",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(space.med)
                        )
                        HorizontalPager(
                            pageCount = seasonalMangaState.size,
                            modifier = Modifier.height(320.dp).fillMaxWidth()
                        ) {
                            val manga = seasonalMangaState[it]

                        }
                    }
                }
                header {
                    Column {
                        Text(
                            text = "Trending",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(space.med)
                        )
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
                        }
                    }
                }
                header {
                    Text(
                        text = "Recently Updated",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(space.med)
                    )
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
                if (sm.loadingRecentManga) {
                    header {
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
    }
}

@Composable
fun SearchItemsList(
    modifier: Modifier = Modifier,
    items: List<DomainManga>,
    state: LazyGridState = rememberLazyGridState(),
    onMangaClick: (manga: DomainManga) -> Unit,
    onBookmarkClick: (manga: DomainManga) -> Unit
) {
    val space = LocalSpacing.current
    LazyVerticalGrid(
        modifier = modifier,
        state = state,
        columns = GridCells.Fixed(2)
    ) {
        items(
            items = items,
            key = { item: DomainManga -> item.id }
        ) { manga ->
            MangaListItem(
                manga = manga,
                modifier = Modifier
                    .padding(space.large)
                    .clickable {
                        onMangaClick(manga)
                    },
                    onBookmarkClick = {
                        onBookmarkClick(manga)
                    }
                )
        }
    }
}

@Composable
fun SearchMangaTopBar(
    searchText: String,
    onSearchTextValueChange: (String) -> Unit,
    onBackArrowClicked: () -> Unit,
) {
    val space = LocalSpacing.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        IconButton(onClick = onBackArrowClicked) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = null
            )
        }
        var focused by remember { mutableStateOf(false) }
        val bgc = MaterialTheme.colorScheme.onBackground
        BasicTextField(
            value = searchText,
            modifier = Modifier.onFocusChanged {
                focused = it.isFocused
            },
            singleLine = true,
            textStyle = TextStyle(
                color = bgc,
                fontSize = 24.sp
            ),
            cursorBrush = SolidColor(bgc),
            onValueChange = onSearchTextValueChange,
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(60.dp)
                        .padding(space.small)
                        .drawBehind {
                            drawLine(
                                color = bgc,
                                start = Offset(0f, this.size.height),
                                end = Offset(this.size.width, this.size.height),
                                strokeWidth = 2f
                            )
                        }
                ) {
                    if (searchText.isEmpty() && !focused) {
                        Text("Search for mangas...", color = bgc, fontSize = 18.sp)
                    }
                    innerTextField()
                }
            }
        )
    }
}

fun LazyGridScope.header(
    content: @Composable LazyGridItemScope.() -> Unit
) {
    item(
        span = { GridItemSpan(this.maxLineSpan) }, content = content)
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