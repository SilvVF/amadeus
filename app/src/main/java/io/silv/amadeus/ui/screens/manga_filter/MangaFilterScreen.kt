package io.silv.amadeus.ui.screens.manga_filter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import io.silv.amadeus.AmadeusScaffold
import io.silv.amadeus.ui.composables.AnimatedBoxShimmer
import io.silv.amadeus.ui.composables.MangaListItem
import io.silv.amadeus.ui.composables.header
import io.silv.amadeus.ui.screens.home.MangaPager
import io.silv.amadeus.ui.screens.manga_view.MangaViewScreen
import io.silv.amadeus.ui.shared.CenterBox
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.repositorys.manga.FilteredMangaRepository
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

        val sm = getScreenModel<MangaFilterSM> { parametersOf(tagId) }
        val navigator = LocalNavigator.current
        val space = LocalSpacing.current
        val timePeriod by sm.timePeriod.collectAsStateWithLifecycle()
        val yearlyItemsState by sm.yearlyFilteredUiState.collectAsStateWithLifecycle()
        val timePeriodItems = sm.timePeriodFilteredPagingFlow.collectAsLazyPagingItems()

        LaunchedEffect(Unit) {
            sm.updateTagId(tagId, tag)
        }

        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
            state = rememberTopAppBarState()
        )

        AmadeusScaffold(
            scrollBehavior = scrollBehavior,
            topBar = {
                TopAppBar(
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(
                            onClick = { navigator?.pop() },
                            modifier = Modifier.padding(space.small)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = null
                            )
                        }
                    },
                    title = {
                        Text(sm.currentTag)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    )
                )
            }
        ) {
            LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
                columns = GridCells.Fixed(2)
            ) {
                header {
                    Column {
                        Text(
                            "${sm.currentTag} trending this year",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(space.large)
                        )
                        when (yearlyItemsState) {
                            YearlyFilteredUiState.Loading -> AnimatedBoxShimmer(
                                Modifier
                                    .height(240.dp)
                                    .fillMaxWidth()
                            )
                            is YearlyFilteredUiState.Success -> MangaPager(
                                mangaList = yearlyItemsState.resources,
                                onMangaClick = {
                                    navigator?.push(
                                        MangaViewScreen(it)
                                    )
                                },
                                onBookmarkClick = {
                                    sm.bookmarkManga(it.id)
                                },
                                onTagClick = { name, tag ->
                                    sm.updateTagId(tag, name)
                                },
                            )
                        }
                        Text(
                            text = "Popularity",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(space.med)
                        )
                        FlowRow {
                            listOf(
                                "all time" to FilteredMangaRepository.TimePeriod.AllTime,
                                "last 6 months" to FilteredMangaRepository.TimePeriod.SixMonths,
                                "last 3 months" to FilteredMangaRepository.TimePeriod.ThreeMonths,
                                "last month" to FilteredMangaRepository.TimePeriod.LastMonth,
                                "last week" to FilteredMangaRepository.TimePeriod.OneWeek
                            ).forEach { (text, time) ->
                                FilterChip(
                                    selected = time == timePeriod,
                                    onClick = { sm.changeTimePeriod(time) },
                                    label = {
                                        Text(text = text)
                                    },
                                    modifier = Modifier.padding(space.small)
                                )
                            }
                        }
                    }
                }
                if(timePeriodItems.loadState.refresh is LoadState.Loading) {
                    items(4) {
                        AnimatedBoxShimmer(Modifier.size(200.dp))
                    }
                } else {
                    items(
                        count = timePeriodItems.itemCount,
                        key = timePeriodItems.itemKey(),
                        contentType = timePeriodItems.itemContentType()
                    ) { i ->
                        timePeriodItems[i]?.let { manga ->
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
                                        sm.updateTagId(it, name)
                                    }
                                },
                                onBookmarkClick = {
                                    sm.bookmarkManga(manga.id)
                                }
                            )
                        }
                    }
                    if (timePeriodItems.loadState.append == LoadState.Loading) {
                        header {
                            CenterBox(Modifier.fillMaxWidth().padding(space.med)) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    if (timePeriodItems.loadState.append is LoadState.Error || timePeriodItems.loadState.refresh is LoadState.Error) {
                        header {
                            CenterBox(Modifier.fillMaxWidth().padding(space.med)) {
                                Button(
                                    onClick = { timePeriodItems.retry() }
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