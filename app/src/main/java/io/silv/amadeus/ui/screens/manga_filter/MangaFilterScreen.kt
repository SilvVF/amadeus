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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import io.silv.amadeus.ui.composables.AnimatedBoxShimmer
import io.silv.amadeus.ui.composables.MangaListItem
import io.silv.amadeus.ui.screens.home.MangaPager
import io.silv.amadeus.ui.screens.home.header
import io.silv.amadeus.ui.screens.manga_view.MangaViewScreen
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.domain.models.SavableManga
import io.silv.manga.domain.repositorys.FilteredMangaRepository
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

class MangaFilterScreen(
    private val tag: String,
    private val tagId: String
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
        val timePeriodItemsState by sm.timePeriodFilteredUiState.collectAsStateWithLifecycle()
        val lazyGridState = rememberLazyGridState()

        LaunchedEffect(Unit) {
            launch {
                snapshotFlow { lazyGridState.firstVisibleItemIndex }.collect { idx ->
                    if (idx >= timePeriodItemsState.resources.lastIndex - 5)  {
                        sm.loadNextPage()
                    }
                }
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            IconButton(
                onClick = { navigator?.pop() },
                modifier = Modifier.padding(space.small)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = null
                )
            }
            LazyVerticalGrid(
                modifier = Modifier.weight(1f),
                state = lazyGridState,
                columns = GridCells.Fixed(2)
            ) {
                header {
                    Column {
                        Text(
                            "$tag trending this year",
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
                                    if (tag != tagId)
                                        navigator?.replace(MangaFilterScreen(name, tag))
                                }
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
                when(timePeriodItemsState) {
                    TimeFilteredUiState.Loading -> items(4) {
                        AnimatedBoxShimmer(Modifier.size(300.dp))
                    }
                    is TimeFilteredUiState.Success -> items(
                        items = timePeriodItemsState.resources,
                        key = { item: SavableManga -> item.id }
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
                                    if (name != tag && it != tagId) {
                                        navigator?.replace(
                                            MangaFilterScreen(name, it)
                                        )
                                    }
                                }
                            },
                            onBookmarkClick = {
                                sm.bookmarkManga(manga.id)
                            }
                        )
                    }
                }
            }
        }
    }
}