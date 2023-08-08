package io.silv.amadeus.ui.screens.home

import android.graphics.drawable.ColorDrawable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import coil.compose.AsyncImage
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import io.silv.amadeus.ui.composables.AmadeusScaffold
import io.silv.amadeus.ui.composables.AnimatedBoxShimmer
import io.silv.amadeus.ui.composables.BlurImageBackground
import io.silv.amadeus.ui.composables.MangaGenreTags
import io.silv.amadeus.ui.composables.MangaListItem
import io.silv.amadeus.ui.composables.TranslatedLanguageTags
import io.silv.amadeus.ui.composables.vertical
import io.silv.amadeus.ui.screens.manga_filter.MangaFilterScreen
import io.silv.amadeus.ui.screens.manga_view.MangaViewScreen
import io.silv.amadeus.ui.screens.search.SearchItems
import io.silv.amadeus.ui.shared.CenterBox
import io.silv.amadeus.ui.shared.noRippleClickable
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.domain.models.SavableManga
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch


class HomeScreen: Screen {

    @OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {

        val sm = getScreenModel<HomeSM>()

        val recentMangaState by sm.recentMangaUiState.collectAsStateWithLifecycle()
        val popularMangaState by sm.popularMangaUiState.collectAsStateWithLifecycle()
        val seasonalMangaState by sm.seasonalMangaUiState.collectAsStateWithLifecycle()
        val refreshingSeasonal by sm.refreshingSeasonal.collectAsStateWithLifecycle()
        val searchMangaState by sm.searchMangaUiState.collectAsStateWithLifecycle()
        val searchQuery by sm.searchQuery.collectAsStateWithLifecycle()
        val space = LocalSpacing.current
        val navigator = LocalNavigator.current
        val recentListState = rememberLazyListState()
        val popularMangaListState = rememberLazyListState()
        val searchListState = rememberLazyGridState()

        LaunchedEffect(popularMangaListState) {
            snapshotFlow { popularMangaListState.firstVisibleItemIndex }.collect { idx ->
                if (idx >= ((popularMangaState as? PaginatedListState.Success<List<SavableManga>>)?.data?.size ?: 0) - 5) {
                    if (searchMangaState !is PaginatedListState.Error<List<SavableManga>>) {
                        sm.loadNextPopularPage()
                    }
                }
            }
        }

        LaunchedEffect(searchListState) {
            snapshotFlow { searchListState.firstVisibleItemIndex }.collect {idx ->
                if (idx >= ((searchMangaState as? PaginatedListState.Success<List<SavableManga>>)?.data?.size ?: 0) - 5) {
                    if (searchMangaState !is PaginatedListState.Error<List<SavableManga>>) {
                        sm.loadNextSearchPage()
                    }
                }
            }
        }

        LaunchedEffect(recentListState) {
            snapshotFlow { recentListState.firstVisibleItemIndex }.collect { idx ->
                if (idx >= ((recentMangaState as? PaginatedListState.Success<List<List<SavableManga>>>)?.data?.size ?: 0) - 5) {
                    if (searchMangaState !is PaginatedListState.Error<List<SavableManga>>) {
                        sm.loadNextRecentPage()
                    }
                }
            }
        }

        var searching by rememberSaveable {
            mutableStateOf(false)
        }

        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(state = rememberTopAppBarState())

        ImageCache(
            list = remember (recentMangaState) {
                (recentMangaState as? PaginatedListState.Success<List<List<SavableManga>>>)?.data?.flatten() ?: emptyList()
            },
            lazyListState = recentListState
        )

        AmadeusScaffold(
            scrollBehavior = scrollBehavior,
            topBar = {
                HomeSearchTopAppBar(
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
                    }
                )
            }
        ) { paddingValues ->
            Column(Modifier.padding(paddingValues)) {
                AnimatedContent(
                    targetState = searching,
                    label = "searching"
                ) {
                    if (it) {
                        SearchItems(
                            modifier = Modifier.fillMaxSize(1f),
                            searchMangaUiState = searchMangaState,
                            gridState = searchListState,
                            onMangaClick = {
                                navigator?.push(MangaViewScreen(it))
                            },
                            onBookmarkClick = {
                                sm.bookmarkManga(it.id)
                            },
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            state = recentListState,
                        ) {
                            seasonalMangaLists(
                                refreshingSeasonal = refreshingSeasonal,
                                seasonalMangaState = seasonalMangaState,
                                onBookmarkClick = {
                                    sm.bookmarkManga(it.id)
                                }
                            )
                            item {
                                TrendingMangaList(
                                    trendingMangaUiState = popularMangaState,
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
                            item {
                                Text(
                                    text = "Recently Updated",
                                    style = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier.padding(space.med)
                                )
                            }
                            recentMangaList(
                                recentMangaStateUiState = recentMangaState,
                                onBookmarkClick = { manga ->
                                    sm.bookmarkManga(manga.id)
                                },
                                onTagClick = { manga, name ->
                                    manga.tagToId[name]?.let {id ->
                                        navigator?.push(
                                            MangaFilterScreen(name, id)
                                        )
                                    }
                                },
                                onMangaClick = { manga ->
                                    navigator?.push(
                                        MangaViewScreen(manga)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


fun LazyListScope.recentMangaList(
    recentMangaStateUiState: PaginatedListState<List<List<SavableManga>>>,
    onTagClick: (manga: SavableManga, name: String) -> Unit,
    onBookmarkClick: (manga: SavableManga) -> Unit,
    onMangaClick: (manga: SavableManga) -> Unit,
) {
    when (recentMangaStateUiState) {
        is PaginatedListState.Error -> {
            recentMangaStateUiState.data.fastForEach {
                item(
                    key = it.joinToString { it.id }
                ) {
                    val space = LocalSpacing.current
                    Row {
                        for(manga in it) {
                            MangaListItem(
                                manga = manga,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(space.large)
                                    .clickable {
                                        onMangaClick(manga)
                                    },
                                onTagClick = { name ->
                                    onTagClick(manga, name)
                                },
                                onBookmarkClick = {
                                    onBookmarkClick(manga)
                                }
                            )
                        }
                    }
                }
            }
        }
        PaginatedListState.Refreshing -> {
            repeat(2) {
                item {
                    Row {
                        repeat(2) {
                            AnimatedBoxShimmer(Modifier.size(300.dp))
                        }
                    }
                }
            }
        }
        is PaginatedListState.Success ->  {
            recentMangaStateUiState.data.fastForEach {
                item(
                    key = it.joinToString { it.id }
                ) {
                    val space = LocalSpacing.current
                    Row {
                        for(manga in it) {
                            MangaListItem(
                                manga = manga,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(space.large)
                                    .clickable {
                                        onMangaClick(manga)
                                    },
                                onTagClick = { name ->
                                    onTagClick(manga, name)
                                },
                                onBookmarkClick = {
                                    onBookmarkClick(manga)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.seasonalMangaLists(
    refreshingSeasonal: Boolean,
    seasonalMangaState: SeasonalMangaUiState,
    onBookmarkClick: (manga: SavableManga) -> Unit,
) {
    item {

        val space = LocalSpacing.current
        val navigator = LocalNavigator.current
        var selectedIndex by rememberSaveable {
            mutableStateOf(0)
        }

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
                        onBookmarkClick(it)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeSearchTopAppBar(
    onSearchText: (String) -> Unit,
    color: Color,
    showTextField: Boolean,
    navigationIconLabel: String,
    navigationIcon: ImageVector,
    onNavigationIconClicked: () -> Unit,
    actions: @Composable (RowScope.() -> Unit),
    scrollBehavior: TopAppBarScrollBehavior,
    searchText: String,
    onSearchChanged: (active: Boolean) -> Unit,
) {
    var alreadyRequestedFocus by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val space = LocalSpacing.current

    TopAppBar(
        title = {
            if (!showTextField) {
                Text("Home")
            }
        },
        modifier = Modifier.statusBarsPadding(),
        navigationIcon = {
            if (showTextField) {
                IconButton(
                    onClick = onNavigationIconClicked,
                ) {
                    Icon(
                        imageVector = navigationIcon,
                        contentDescription = navigationIconLabel
                    )
                }
            }
        },
        actions = {
            AnimatedVisibility(visible = showTextField, enter = fadeIn() + slideInVertically(), exit = fadeOut() + slideOutVertically()) {
                OutlinedTextField(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = space.small, bottom = space.small, start = space.xlarge)
                        .focusRequester(focusRequester),
                    value = searchText,
                    placeholder = { Text("Search Manga...") },
                    onValueChange = {
                        onSearchText(it)
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        cursorColor = LocalContentColor.current,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    trailingIcon = {
                        AnimatedVisibility(visible = searchText.isNotBlank(), enter = fadeIn(), exit = fadeOut()) {
                            IconButton(
                                onClick = { onSearchText("") }
                            ) {
                                Icon(imageVector = Icons.Filled.Clear, contentDescription = null)
                            }
                        }

                    },
                    maxLines = 1,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            onSearchText(searchText)
                        },
                    ),
                )
                LaunchedEffect(Unit) {
                    if (!alreadyRequestedFocus) {
                        focusRequester.requestFocus()
                        alreadyRequestedFocus = true
                    }
                    if (searchText.isNotBlank()) {
                        onSearchText(searchText)
                    }
                }
            }
            val icon = when (showTextField) {
                true -> Icons.Filled.SearchOff
                false -> Icons.Filled.Search
            }
            IconButton(
                onClick = {
                    onSearchText("")
                    alreadyRequestedFocus = false
                    onSearchChanged(!showTextField)
                },
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null
                )
            }
            actions()
        },
        colors = topAppBarColors(
            containerColor = color,
            scrolledContainerColor = color,
        ),
        scrollBehavior = scrollBehavior,
    )
}


fun shouldShowBottomBar(windowSizeClass: WindowSizeClass?): Boolean {
    return (windowSizeClass?.widthSizeClass ?: return true) == WindowWidthSizeClass.Compact
}


@OptIn(FlowPreview::class)
@Composable
fun  ImageCache(
    list: List<SavableManga>,
    lazyListState: LazyListState,
    toDisk: Boolean = false
) {
    val imageLoader = LocalContext.current.imageLoader
    val context = LocalContext.current

    val prevIds = remember {
        mutableStateListOf<String>()
    }

    LaunchedEffect(Unit) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }.debounce(10).collect {
            val preload = (runCatching { list.subList(it, it + 30) }.getOrNull() ?: emptyList()).filterNot { it.id in prevIds }
            if (toDisk) {
                for(manga in preload) {
                    prevIds.add(manga.id)
                    val request = ImageRequest.Builder(context)
                        .data(manga.coverArt)
                        // Disable reading from/writing to the memory cache.
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        // Set a custom `Decoder.Factory` that skips the decoding step.
                        .decoderFactory { _, _, _ ->
                            Decoder { DecodeResult(ColorDrawable(Color.Black.toArgb()), false) }
                        }
                        .build()
                    imageLoader.enqueue(request)
                }
            } else {
                for (manga in preload) {
                    prevIds.add(manga.id)
                    val request = ImageRequest.Builder(context)
                        .data(manga.coverArt)
                        // Optional, but setting a ViewSizeResolver will conserve memory by limiting the size the image should be preloaded into memory at.
                        .build()
                    imageLoader.enqueue(request)
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
fun TrendingMangaList(
    trendingMangaUiState: PaginatedListState<List<SavableManga>>,
    state: LazyListState = rememberLazyListState(),
    onMangaClick: (manga: SavableManga) -> Unit,
    onBookmarkClick: (manga: SavableManga) -> Unit
) {
    val space = LocalSpacing.current
    val navigator = LocalNavigator.current
    when (trendingMangaUiState) {
        is PaginatedListState.Error -> {
            Column {
                Text(
                    text = "Trending",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(space.med)
                )
                LazyRow(
                    state = state,
                    modifier = Modifier
                        .wrapContentHeight()
                        .fillMaxWidth()
                ) {
                    trendingMangaUiState.data.fastForEachIndexed { i, manga ->
                        item(
                            key = manga.id
                        ) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                Text(
                                    modifier = Modifier
                                        .vertical()
                                        .rotate(-90f)
                                        .padding(space.small)
                                        .offset(x = 60.dp)
                                        .widthIn(0.dp, 200.dp),
                                    text = "${i + 1} ${manga.titleEnglish}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                )
                                MangaListItem(
                                    manga = manga,
                                    modifier = Modifier
                                        .padding(space.large)
                                        .width(220.dp)
                                        .height(295.dp)
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
                    }
                }
                Text(text = trendingMangaUiState.message)
            }
        }
        PaginatedListState.Refreshing -> {
            Row {
                repeat(5) {
                    AnimatedBoxShimmer(Modifier.size(200.dp))
                }
            }
        }
        is PaginatedListState.Success -> {
            ImageCache(list = trendingMangaUiState.data, lazyListState =state )

            Column {
                Text(
                    text = "Trending",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(space.med)
                )
                LazyRow(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    trendingMangaUiState.data.fastForEachIndexed { i, manga ->
                        item(
                            key = manga.id
                        ) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                Text(
                                    modifier = Modifier
                                        .vertical()
                                        .rotate(-90f)
                                        .padding(space.small)
                                        .offset(x = 60.dp)
                                        .widthIn(0.dp, 200.dp),
                                    text = "${i + 1} ${manga.titleEnglish}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                )
                                MangaListItem(
                                    manga = manga,
                                    modifier = Modifier
                                        .padding(space.large)
                                        .width(220.dp)
                                        .height(295.dp)
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
                    }
                }
                if (trendingMangaUiState.loading) {
                    CircularProgressIndicator()
                } else if (trendingMangaUiState.end) {
                    Text(text = "end of pagination")
                }
            }
        }
    }
}



