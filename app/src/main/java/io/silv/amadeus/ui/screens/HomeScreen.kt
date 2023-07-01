package io.silv.amadeus.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkManager
import cafe.adriel.voyager.core.model.coroutineScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import io.silv.amadeus.domain.models.ChapterImages
import io.silv.amadeus.domain.models.DomainManga
import io.silv.amadeus.domain.models.DomainVolume
import io.silv.amadeus.domain.repos.MangaRepo
import io.silv.amadeus.filterUnique
import io.silv.amadeus.ui.composables.HomeTopBar
import io.silv.amadeus.ui.composables.MangaListItem
import io.silv.amadeus.ui.screens.manga_view.MangaViewScreen
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.ktor_response_mapper.suspendOnFailure
import io.silv.ktor_response_mapper.suspendOnSuccess
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class HomeSM(
    private val mangaRepo: MangaRepo,
    private val workManager: WorkManager
): AmadeusScreenModel<HomeEvent, HomeState>(HomeState()) {

    init {
        coroutineScope.launch {
            mutableState.value = HomeState(loadingNextPage = true)
            mangaRepo.getMangaWithArt()
                .suspendOnSuccess {
                    mutableState.emit(
                        HomeState(
                            data = this.data,
                            loadingNextPage = false
                        )
                    )
                }
                .suspendOnFailure {
                    mutableState.emit(
                        HomeState(loadingNextPage = false)
                    )
                }
        }
    }


    private var nextPageJob: Job? = null

    fun nextPage() {
        if (nextPageJob != null) { return }
        mutableState.value = mutableState.value.copy(loadingNextPage = true)
        nextPageJob = coroutineScope.launch {
            mangaRepo.getMangaWithArt()
                .suspendOnSuccess {
                    mutableState.emit(
                        HomeState(
                            data = (mutableState.value.data + this.data).filterUnique { it.id },
                            loadingNextPage = false
                        )
                    )
                }
                .suspendOnFailure {
                    mutableState.emit(
                        HomeState(loadingNextPage = false)
                    )
                }
            nextPageJob = null
        }
    }
}

sealed interface HomeEvent {
    data class Navigate(val volumes: List<DomainVolume>, val manga: DomainManga): HomeEvent
}

@Immutable
data class HomeState(
    val data: List<DomainManga> = emptyList(),
    val loadingNextPage: Boolean = false,
    val chapterImages: ChapterImages? = null
)

class HomeScreen: Screen {

    @Composable
    override fun Content() {

        val sm = getScreenModel<HomeSM>()

        val state by sm.state.collectAsStateWithLifecycle()

        val ctx = LocalContext.current
        val space = LocalSpacing.current
        val navigator = LocalNavigator.current
        val gridState = rememberLazyGridState()

        LaunchedEffect(Unit) {
            snapshotFlow { gridState.firstVisibleItemIndex }.collect {
                if (it == gridState.layoutInfo.totalItemsCount - gridState.layoutInfo.visibleItemsInfo.size) {
                    sm.nextPage()
                }
            }
        }

        Scaffold(
            topBar = {
                HomeTopBar(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        ) { paddingValues ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyVerticalGrid(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    state = gridState,
                    columns = GridCells.Fixed(2)
                ) {
                    items(
                        items = state.data,
                        key = { m: DomainManga -> m.id }
                    ) { manga ->
                        MangaListItem(
                            manga = manga,
                            modifier = Modifier
                                .padding(space.large)
                                .clickable {
                                    navigator?.push(
                                        MangaViewScreen(manga)
                                    )
                                }
                        )
                    }
                }
                if (state.loadingNextPage) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Text(text = "Loading next page")
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

