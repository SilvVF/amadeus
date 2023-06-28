package io.silv.amadeus.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.coroutineScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.silv.amadeus.domain.models.Manga
import io.silv.amadeus.domain.repos.MangaRepo
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.ktor_response_mapper.suspendOnSuccess
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class HomeSM(
    private val mangaRepo: MangaRepo
): AmadeusScreenModel<HomeEvent, HomeState>(HomeState()) {

    init {
        coroutineScope.launch {
            mangaRepo.getMangaWithArt()
                .suspendOnSuccess {
                    mutableState.emit(
                        HomeState(
                            data = this.data
                        )
                    )
                }
        }
    }

    var nextPageJob: Job? = null

    fun nextPage() {
        if (nextPageJob != null) {
            return
        }
        nextPageJob = coroutineScope.launch {
            mangaRepo.getMangaWithArt()
                .suspendOnSuccess {
                    mutableState.emit(
                        HomeState(
                            data = mutableState.value.data + this.data
                        )
                    )
                }
            nextPageJob = null
        }
    }
}

sealed interface HomeEvent

data class HomeState(
    val data: List<Manga> = emptyList()
)

class HomeScreen: Screen {

    @Composable
    override fun Content() {

        val sm = getScreenModel<HomeSM>()

        val state by sm.state.collectAsState()

        val ctx = LocalContext.current

        val gridState = rememberLazyGridState()

        LaunchedEffect(Unit) {
            snapshotFlow { gridState.firstVisibleItemIndex }.collect {
                if (it == gridState.layoutInfo.totalItemsCount - gridState.layoutInfo.visibleItemsInfo.size) {
                    sm.nextPage()
                }
            }
        }

        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            state = gridState,
            columns = GridCells.Fixed(2)
        ) {
                items(
                    items = state.data,
                    key = { item -> item.id }
                ) { manga ->
                    Column {
                        AsyncImage(
                            model = ImageRequest.Builder(ctx)
                                .data(manga.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(280.dp)
                        )
                        Text(
                            text = manga.title
                        )
                        Text(
                            text = manga.altTitle
                        )

                        val mangaText by remember {
                            derivedStateOf {
                                if (manga.description.length <= 50)
                                    manga.description
                                else
                                    manga.description.split(" ").take(50).joinToString()
                            }
                        }

                        Text(
                            text = mangaText
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
        }

    }
}

