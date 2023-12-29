package io.silv.manga.manga_filter

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil.compose.AsyncImage
import io.silv.common.model.TimePeriod
import io.silv.domain.manga.model.Manga
import io.silv.navigation.SharedScreen
import io.silv.navigation.push
import io.silv.navigation.replace
import io.silv.ui.CenterBox
import io.silv.ui.composables.AnimatedBoxShimmer
import io.silv.ui.composables.BlurImageBackground
import io.silv.ui.composables.MangaGenreTags
import io.silv.ui.composables.MangaListItem
import io.silv.ui.composables.TranslatedLanguageTags
import io.silv.ui.theme.LocalSpacing
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

class MangaFilterScreen(
    private val tag: String,
    private val tagId: String,
) : Screen {
    override val key: ScreenKey
        get() = super.key + tag + tagId

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val sm = getScreenModel<MangaFilterScreenModel> { parametersOf(tagId) }
        val navigator = LocalNavigator.currentOrThrow
        val space = LocalSpacing.current
        val timePeriod by sm.timePeriod.collectAsStateWithLifecycle()
        val yearlyItemsState = sm.state.collectAsStateWithLifecycle().value

        val scrollBehavior =
            TopAppBarDefaults.enterAlwaysScrollBehavior(
                state = rememberTopAppBarState(),
            )
        val timePeriodPager by sm.timePeriodFilteredPagingFlow.collectAsStateWithLifecycle()
        val timePeriodItems = timePeriodPager.collectAsLazyPagingItems()

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(
                            onClick = { navigator.pop() },
                            modifier = Modifier.padding(space.small),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    },
                    title = {
                        Text(tag)
                    },
                    colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                )
            },
        ) { paddingValues ->
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                columns = GridCells.Fixed(2),
                contentPadding = paddingValues,
            ) {
                item(
                    key = "time-period-tags",
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    Column {
                        Text(
                            "$tag trending this year",
                            style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            modifier = Modifier.padding(space.large),
                        )
                        when (yearlyItemsState) {
                            YearlyFilteredUiState.Loading ->
                                AnimatedBoxShimmer(
                                    Modifier
                                        .height(240.dp)
                                        .fillMaxWidth(),
                                )
                            is YearlyFilteredUiState.Success -> {
                                YearlyMangaPager(
                                    mangaList = yearlyItemsState.resources,
                                    onMangaClick = {},
                                    onBookmarkClick = {},
                                    onTagClick = { name, id ->
                                        if (id != tagId) {
                                            navigator.replace(SharedScreen.MangaFilter(name, id))
                                        }
                                    },
                                )
                            }
                        }
                        Text(
                            text = "Popularity",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(space.med),
                        )
                        FlowRow {
                            listOf(
                                "all time" to TimePeriod.AllTime,
                                "last 6 months" to TimePeriod.SixMonths,
                                "last 3 months" to TimePeriod.ThreeMonths,
                                "last month" to TimePeriod.LastMonth,
                                "last week" to TimePeriod.OneWeek,
                            ).forEach { (text, time) ->
                                ElevatedFilterChip(
                                    selected = time == timePeriod,
                                    onClick = { sm.changeTimePeriod(time) },
                                    label = {
                                        Text(text = text)
                                    },
                                    modifier = Modifier.padding(space.small),
                                )
                            }
                        }
                    }
                }
                if (timePeriodItems.loadState.refresh is LoadState.Loading) {
                    item("loading-refresh", span = { GridItemSpan(maxLineSpan) }) {
                        CenterBox(Modifier.fillMaxWidth().height(200.dp)) {
                            CircularProgressIndicator()
                        }
                    }
                } else {
                    items(
                        count = timePeriodItems.itemCount,
                        key = timePeriodItems.itemKey(),
                        contentType = timePeriodItems.itemContentType(),
                    ) { i ->

                        val item = timePeriodItems[i]?.collectAsStateWithLifecycle()

                        item?.value?.let { manga ->
                            MangaListItem(
                                manga = manga,
                                modifier =
                                Modifier
                                    .padding(space.small)
                                    .aspectRatio(2f / 3f)
                                    .clickable {
                                        navigator.push(SharedScreen.MangaView(manga.id))
                                    },
                                onTagClick = { name ->
                                    manga.tagToId[name]?.let { id ->
                                        navigator.replace(SharedScreen.MangaFilter(name, id))
                                    }
                                },
                                onBookmarkClick = {
                                    sm.bookmarkManga(manga.id)
                                },
                            )
                        }
                    }
                    if (timePeriodItems.loadState.append == LoadState.Loading) {
                        item(
                            key = "append-loading",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            CenterBox(Modifier.fillMaxWidth()) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    if (timePeriodItems.loadState.append is LoadState.Error || timePeriodItems.loadState.refresh is LoadState.Error) {
                        item(
                            key = "retry-loading",
                        ) {
                            CenterBox(
                                Modifier.fillMaxWidth(),
                            ) {
                                Button(
                                    onClick = { timePeriodItems.retry() },
                                ) {
                                    Text("Retry loading items")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalFoundationApi::class)
@Composable
fun YearlyMangaPager(
    mangaList: ImmutableList<StateFlow<Manga>>,
    onMangaClick: (manga: Manga) -> Unit,
    onBookmarkClick: (manga: Manga) -> Unit,
    onTagClick: (name: String, id: String) -> Unit,
) {
    val space = LocalSpacing.current

    val pagerState =
        rememberPagerState(
            initialPage = 0,
            initialPageOffsetFraction = 0f,
            pageCount = {
                mangaList.size
            },
        )
    val scope = rememberCoroutineScope()

    HorizontalPager(
        state = pagerState,
        modifier =
        Modifier
            .height(240.dp)
            .fillMaxWidth(),
    ) { page ->

        val manga by mangaList[page].collectAsStateWithLifecycle()

        BlurImageBackground(
            modifier =
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    onMangaClick(manga)
                },
            url = manga.coverArt,
        ) {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .padding(space.med),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    CenterBox(Modifier.height(230.dp)) {
                        AsyncImage(
                            model = manga,
                            contentDescription = null,
                            contentScale = ContentScale.Inside,
                            modifier =
                            Modifier
                                .fillMaxWidth(0.4f),
                        )
                    }
                    Spacer(modifier = Modifier.width(space.med))
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text(
                            text = manga.titleEnglish,
                            style =
                            MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onBackground,
                            ),
                            maxLines = 2,
                            fontSize = 20.sp,
                            overflow = TextOverflow.Ellipsis,
                        )
                        TranslatedLanguageTags(
                            tags = remember(manga.id) {
                                manga.availableTranslatedLanguages.toImmutableList()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        MangaGenreTags(
                            tags = manga.tags,
                            modifier = Modifier.fillMaxWidth(),
                            onTagClick = { name ->
                                manga.tagToId[name]?.let {
                                    onTagClick(name, it)
                                }
                            },
                        )
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.End,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                IconButton(onClick = { onBookmarkClick(manga) }) {
                                    Icon(
                                        imageVector =
                                        if (manga.inLibrary) {
                                            Icons.Filled.Favorite
                                        } else {
                                            Icons.Outlined.FavoriteBorder
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onBackground,
                                    )
                                }
                                Text(
                                    text =
                                    if (manga.inLibrary) {
                                        "In library"
                                    } else {
                                        "Add to library"
                                    },
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                            ) {
                                Text(
                                    text = "NO.${page + 1}",
                                    style =
                                    MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                    ),
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
                                        tint = MaterialTheme.colorScheme.onBackground,
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            pagerState.animateScrollToPage(page + 1)
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onBackground,
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
