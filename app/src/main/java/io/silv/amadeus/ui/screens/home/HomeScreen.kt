package io.silv.amadeus.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.silv.amadeus.ui.composables.AnimatedBoxShimmer
import io.silv.amadeus.ui.composables.ConfirmCloseAppPopup
import io.silv.amadeus.ui.composables.HomeTopBar
import io.silv.amadeus.ui.composables.MangaGenreTags
import io.silv.amadeus.ui.composables.MangaListItem
import io.silv.amadeus.ui.composables.TranslatedLanguageTags
import io.silv.amadeus.ui.screens.manga_filter.MangaFilterScreen
import io.silv.amadeus.ui.screens.manga_view.MangaViewScreen
import io.silv.amadeus.ui.screens.search.SearchMangaUiState
import io.silv.amadeus.ui.shared.CenterBox
import io.silv.amadeus.ui.shared.noRippleClickable
import io.silv.amadeus.ui.theme.LocalBottomBarVisibility
import io.silv.amadeus.ui.theme.LocalPaddingValues
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.domain.models.SavableManga
import kotlinx.coroutines.launch


class HomeScreen: Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val sm = getScreenModel<HomeSM>()

        val recentMangaState by sm.recentMangaUiState.collectAsStateWithLifecycle()
        val popularMangaState by sm.popularMangaUiState.collectAsStateWithLifecycle()
        val seasonalMangaState by sm.seasonalMangaUiState.collectAsStateWithLifecycle()
        val loadingPopular by sm.loadingPopularManga.collectAsStateWithLifecycle()
        val loadingRecent by sm.loadingRecentManga.collectAsStateWithLifecycle()
        val refreshingSeasonal by sm.refreshingSeasonal.collectAsStateWithLifecycle()
        val searchMangaState by sm.searchMangaUiState.collectAsStateWithLifecycle()
        val searchQuery by sm.searchQuery.collectAsStateWithLifecycle()
        val tagsState by sm.tagsUiState.collectAsStateWithLifecycle()

        val space = LocalSpacing.current
        val navigator = LocalNavigator.current
        val bottomBarPadding by LocalPaddingValues.current
        val recentListState = rememberLazyGridState()
        val popularMangaListState = rememberLazyListState()
        val searchListState = rememberLazyGridState()
        var bottomBarVisibility by LocalBottomBarVisibility.current

        LaunchedEffect(popularMangaListState) {
            snapshotFlow { popularMangaListState.firstVisibleItemIndex }.collect { idx ->
                if (idx >= popularMangaState.size - 5) {
                    sm.loadNextPopularPage()
                }
            }
        }

        LaunchedEffect(searchListState) {
            snapshotFlow { searchListState.firstVisibleItemIndex }.collect {idx ->
                val listSize = ((searchMangaState as? SearchMangaUiState.Success)?.results?.size ?: 0)
                if (idx >= listSize - 5) {
                    sm.loadNextSearchPage()
                }
            }
        }

        LaunchedEffect(recentListState) {
            bottomBarVisibility = true
            snapshotFlow { recentListState.firstVisibleItemIndex }.collect { idx ->
                if (idx >= recentMangaState.size - 5 - 3) {
                    sm.loadNextRecentPage()
                }
            }
        }

        ConfirmCloseAppPopup()

        Column(
            Modifier
                .fillMaxSize()
                .padding(bottomBarPadding)
        ) {
            HomeTopBar(
                onBookmarkClick = {
                    sm.bookmarkManga(it.id)
                },
                onMangaClick = {
                    navigator?.push(
                        MangaViewScreen(it)
                    )
                },
                onSearchQueryChange = sm::updateSearchQuery,
                searchItems = searchMangaState,
                searchQuery = searchQuery,
                searchGridState = searchListState
            )

            var selectedIndex by rememberSaveable {
                mutableStateOf(0)
            }

            LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                state = recentListState,
                columns = GridCells.Fixed(2)
            ) {
                header {
                    Column {
                        Text(
                            "seasonal lists",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(space.xs)
                        )
                        if (refreshingSeasonal) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                repeat(4) {
                                    AnimatedBoxShimmer(
                                        Modifier
                                            .weight(1f)
                                            .height(40.dp))
                                }
                            }
                            AnimatedBoxShimmer(
                                Modifier
                                    .height(240.dp)
                                    .fillMaxWidth()
                            )
                        } else {
                            LazyRow {
                                itemsIndexed(
                                    seasonalMangaState.seasonalLists,
                                    key = { _, list -> list.id }
                                ) { index, seasonalList ->
                                    FilterChip(
                                        selected = index == selectedIndex,
                                        onClick = { selectedIndex = index },
                                        label = {
                                            Text(
                                                "${seasonalList.season.name}  ${seasonalList.year.toString().takeLast(2)}",
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
                            MangaPager(
                                mangaList = seasonalMangaState.seasonalLists.getOrNull(selectedIndex)?.mangas ?: emptyList(),
                                onTagClick = { name , id ->
                                    navigator?.push(
                                        MangaFilterScreen(name, id)
                                    )
                                },
                                onMangaClick = {
                                    navigator?.push(
                                        MangaViewScreen(it)
                                    )
                                },
                                onBookmarkClick = {
                                    sm.bookmarkManga(it.id)
                                },
                            )
                        }
                    }
                }
                header {
                    TrendingMangaList(
                        trendingManga = popularMangaState,
                        loading = loadingPopular,
                        state = popularMangaListState,
                        onMangaClick = {
                            navigator?.push(
                                MangaViewScreen(it)
                            )
                        },
                        onBookmarkClick = {
                            sm.bookmarkManga(it.id)
                        }
                    )
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
                    key = {item: SavableManga -> item.id }
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
                        onTagClick = { name ->
                            manga.tagToId[name]?.let {
                                navigator?.push(
                                    MangaFilterScreen(name, it)
                                )
                            }
                        },
                        onBookmarkClick = { sm.bookmarkManga(manga.id) }
                    )
                }
                if (loadingRecent) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MangaPager(
    mangaList: List<SavableManga>,
    onMangaClick: (manga: SavableManga) -> Unit,
    onBookmarkClick: (manga: SavableManga) -> Unit,
    onTagClick: (name: String, id: String) -> Unit,
) {
    val space = LocalSpacing.current
    val context = LocalContext.current
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()

    HorizontalPager(
        pageCount = mangaList.size,
        state = pagerState,
        modifier = Modifier
            .height(240.dp)
            .fillMaxWidth()
    ) { page ->

            val manga = mangaList[page]

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

                val lazyListState = rememberLazyListState()
                var showDescription by remember {
                    mutableStateOf(false)
                }

                LazyColumn(
                    state = lazyListState
                ) {
                    item {
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
                                    model = ImageRequest.Builder(context)
                                        .data(manga.coverArt)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Inside,
                                    modifier = Modifier
                                        .fillMaxWidth(0.4f)
                                )
                            }
                            Spacer(modifier = Modifier.width(space.med))
                            Column {
                                Text(
                                    text = manga.titleEnglish,
                                    style = MaterialTheme.typography.titleMedium,
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
                                Row(
                                    Modifier.weight(1f),
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(onClick = { onBookmarkClick(manga) }) {
                                        if (manga.bookmarked) Icon(
                                            imageVector = Icons.Filled.BookmarkRemove,
                                            contentDescription = null
                                        )
                                        else Icon(
                                            imageVector = Icons.Outlined.BookmarkAdd,
                                            contentDescription = null
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
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    pagerState.animateScrollToPage(page - 1)
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.KeyboardArrowLeft,
                                                contentDescription = null
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
                                                contentDescription = null
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${if (showDescription) "hide" else "show"} description",
                                    modifier = Modifier
                                        .noRippleClickable {
                                            showDescription = !showDescription
                                            if (showDescription) {
                                                scope.launch {
                                                    lazyListState.animateScrollToItem(1)
                                                }
                                            }
                                        }
                                        .align(Alignment.End)
                                )
                            }
                        }
                    }
                    if (showDescription) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Description",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                IconButton(
                                    onClick = {
                                       showDescription = false
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowUp,
                                        contentDescription = null
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(space.med))
                            Text(text = manga.description)
                        }
                }
            }
        }
    }
}

@Composable
fun BlurImageBackground(modifier: Modifier, url: String, content: @Composable () -> Unit = {}) {
    val context = LocalContext.current
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val screenHeight = LocalConfiguration.current.screenHeightDp
    Box(
        modifier = modifier
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .blur(10.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(url)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(screenWidth.dp)
                    .height(screenHeight.dp)
            )
        }
        Box(modifier = Modifier
            .fillMaxSize()
            .background(
                Color.Black.copy(alpha = 0.8f)
            )
        )
        content()
    }
}

@Composable
fun TrendingMangaList(
    trendingManga: List<SavableManga>,
    loading: Boolean,
    state: LazyListState = rememberLazyListState(),
    onMangaClick: (manga: SavableManga) -> Unit,
    onBookmarkClick: (manga: SavableManga) -> Unit
) {
    val space = LocalSpacing.current
    val navigator = LocalNavigator.current
    Column {
        Text(
            text = "Trending",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(space.med)
        )
        LazyRow(
            state = state,
        ) {
            itemsIndexed(
                trendingManga,
                key = { _, manga ->  manga.id }
            ) { i, manga ->
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
                            .height(330.dp)
                            .clickable {
                                onMangaClick(manga)
                            },
                        onTagClick = { name ->
                            manga.tagToId[name]?.let {
                                navigator?.push(
                                    MangaFilterScreen(name, it)
                                )
                            }
                        },
                        onBookmarkClick = { onBookmarkClick(manga) }
                    )
                }
            }
            if (loading) {
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