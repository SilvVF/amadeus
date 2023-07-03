package io.silv.amadeus.ui.screens

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkManager
import cafe.adriel.voyager.core.model.coroutineScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.FadeTransition
import io.silv.manga.domain.models.ChapterImages
import io.silv.manga.domain.models.DomainManga
import io.silv.manga.domain.repos.MangaRepo
import io.silv.amadeus.ui.composables.AnimatedBoxShimmer
import io.silv.amadeus.ui.composables.HomeTopBar
import io.silv.amadeus.ui.composables.MangaListItem
import io.silv.amadeus.ui.screens.manga_view.MangaViewScreen
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.amadeus.ui.theme.LocalBottomBarVisibility
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.core.filterUnique
import io.silv.ktor_response_mapper.suspendOnFailure
import io.silv.ktor_response_mapper.suspendOnSuccess
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeSM(
    private val mangaRepo: MangaRepo,
    private val workManager: WorkManager
): AmadeusScreenModel<HomeEvent, HomeState>(HomeState()) {

    var offset = 0

    init {
        coroutineScope.launch {
            mutableState.value = HomeState(loadingNextPage = true)
            mangaRepo.getMangaList()
                .suspendOnSuccess {
                    mutableState.emit(
                        HomeState(
                            mangaList = this.data,
                            loadingNextPage = false
                        )
                    )
                    offset += 50
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
            mangaRepo.getMangaList(offset)
                .suspendOnSuccess {
                    mutableState.update {state ->
                        state.copy(
                            mangaList = (state.mangaList + data).filterUnique { it.id },
                            loadingNextPage = false
                        )
                    }
                    offset += 50
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

sealed interface HomeEvent

@Immutable
data class HomeState(
    val mangaList: List<DomainManga> = emptyList(),
    val bookmarkedManga: List<DomainManga> = emptyList(),
    val startedManga: List<DomainManga> = emptyList(),
    val loadingNextPage: Boolean = false,
    val chapterImages: ChapterImages? = null
)

object HomeTab: Tab {

    override val options: TabOptions
        @Composable
        get() {
            val title = "Home"
            val icon = rememberVectorPainter(Icons.Filled.Home)

            return remember {
                TabOptions(
                    index = 0u,
                    title = title,
                    icon = icon
                )
            }
        }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    override fun Content() {
        Navigator(HomeScreen()) {
            FadeTransition(navigator = it)
        }
    }
}


class HomeScreen: Screen {

    @Composable
    override fun Content() {
        val sm = getScreenModel<HomeSM>()

        val state by sm.state.collectAsStateWithLifecycle()

        val ctx = LocalContext.current
        val space = LocalSpacing.current
        val navigator = LocalNavigator.current
        val gridState = rememberLazyGridState()
        var bottomBarVisibility by LocalBottomBarVisibility.current

        LaunchedEffect(Unit) {
            bottomBarVisibility = true
            snapshotFlow { gridState.firstVisibleItemIndex }.collect {
                if (it == gridState.layoutInfo.totalItemsCount - gridState.layoutInfo.visibleItemsInfo.size) {
                    sm.nextPage()
                }
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .navigationBarsPadding()
        ) {
            HomeTopBar(modifier = Modifier.fillMaxWidth())
            LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                state = gridState,
                columns = GridCells.Fixed(2)
            ) {
                items(
                    items = state.mangaList,
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
                if (state.loadingNextPage) {
                    items(6) {
                        AnimatedBoxShimmer(Modifier.size(220.dp))
                    }
                }
            }
        }
    }
}
