package io.silv.manga.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.silv.common.model.TimePeriod
import io.silv.navigation.SharedScreen
import io.silv.navigation.push
import io.silv.navigation.replace
import io.silv.ui.CenterBox
import io.silv.ui.composables.mangaGrid
import io.silv.ui.composables.mangaList
import io.silv.ui.layout.TopAppBarWithBottomContent
import io.silv.ui.theme.LocalSpacing


class MangaFilterScreen(
    private val tag: String,
    private val tagId: String,
) : Screen {
    override val key: ScreenKey
        get() = "MangaFilterScreen_${tag}_${tagId}"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val sm = rememberScreenModel { MangaFilterScreenModel(tagId = tagId) }
        val navigator = LocalNavigator.currentOrThrow
        val space = LocalSpacing.current

        val scrollBehavior =
            TopAppBarDefaults.enterAlwaysScrollBehavior(
                state = rememberTopAppBarState(),
            )
        val timePeriodPager by sm.timePeriodFilteredPagingFlow.collectAsStateWithLifecycle()
        val timePeriodItems = timePeriodPager.collectAsLazyPagingItems()
        val state by sm.state.collectAsStateWithLifecycle()

        var optionsVisible by rememberSaveable { mutableStateOf(false) }

        if (optionsVisible) {
            FilterDisplayOptionsBottomSheet(
                settings = state.settings,
                optionsTitle = {
                    Text("Filter options")
                },
                onDismissRequest = {
                    optionsVisible = !optionsVisible
                }
            )
        }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBarWithBottomContent(
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(
                            onClick = { navigator.pop() },
                            modifier = Modifier.padding(space.small),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    },
                    title = {
                        val timePeriodString = remember(state.timePeriod) {
                            when (state.timePeriod) {
                                TimePeriod.OneYear -> "this year"
                                TimePeriod.SixMonths -> "last six months"
                                TimePeriod.ThreeMonths -> "last three months"
                                TimePeriod.LastMonth -> "last month"
                                TimePeriod.OneWeek -> "last week"
                                TimePeriod.AllTime -> "all time"
                            }
                        }
                        Text(
                            "$tag trending $timePeriodString",
                            style =
                                MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            modifier = Modifier.padding(space.large)
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { optionsVisible = !optionsVisible },
                            modifier = Modifier.padding(space.small),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = null,
                            )
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent,
                        ),
                    bottomContent = {
                        val items = remember {
                            listOf(
                                "all time" to TimePeriod.AllTime,
                                "last year" to TimePeriod.OneYear,
                                "last 6 months" to TimePeriod.SixMonths,
                                "last 3 months" to TimePeriod.ThreeMonths,
                                "last month" to TimePeriod.LastMonth,
                                "last week" to TimePeriod.OneWeek,
                            )
                        }
                        LazyRow {
                            items.fastForEach { (text, time) ->
                                item(
                                    key = time.ordinal
                                ) {
                                    ElevatedFilterChip(
                                        selected = time == state.timePeriod,
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
                )
            },
        ) { paddingValues ->
            when {
                timePeriodItems.loadState.refresh is LoadState.Loading -> {
                    CenterBox(Modifier.fillMaxSize()) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                }

                timePeriodItems.itemCount == 0 &&
                        timePeriodItems.loadState.refresh != LoadState.Loading
                        && timePeriodItems.loadState.append != LoadState.Loading -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(space.xlarge),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.surfaceTint,
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.16f)
                        )
                        Text(
                            "No items to display try adjusting the time period.",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                state.settings.useList -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = paddingValues
                    ) {
                        mangaList(
                            manga = timePeriodItems,
                            onFavoriteClick = { sm::toggleFavorite.invoke(it.id) },
                            onMangaClick = { navigator.push(SharedScreen.MangaView(it.id)) }
                        )
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        modifier = Modifier.fillMaxSize(),
                        columns = GridCells.Fixed(state.settings.gridCells),
                        contentPadding = paddingValues,
                    ) {
                        mangaGrid(
                            manga = timePeriodItems,
                            cardType = state.settings.cardType,
                            onTagClick = { manga, tag ->
                                manga.tagToId[tag]?.let { id ->
                                    navigator.replace(
                                        SharedScreen.MangaFilter(tag, id)
                                    )
                                }
                            },
                            onBookmarkClick = { sm::toggleFavorite.invoke(it.id) },
                            onMangaClick = { navigator.push(SharedScreen.MangaView(it.id)) }
                        )
                    }
                }
            }
        }
    }
}

