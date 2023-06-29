package io.silv.amadeus.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.LifecycleOwner
import cafe.adriel.voyager.core.model.coroutineScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.silv.amadeus.domain.models.ChapterImages
import io.silv.amadeus.domain.models.Manga
import io.silv.amadeus.domain.repos.MangaRepo
import io.silv.amadeus.filterUnique
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.ktor_response_mapper.suspendOnFailure
import io.silv.ktor_response_mapper.suspendOnSuccess
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class HomeSM(
    private val mangaRepo: MangaRepo
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


    fun loadImages(mangaId: String, lifecycleOwner: LifecycleOwner) = coroutineScope.launch {
        mangaRepo.getChapterImages(mangaId, 1, 1, lifecycleOwner)
            .collect {
                it.also { println("zzz" + it) }
                mutableState.emit(
                    mutableState.value.copy(
                        chapterImages = it
                    )
                )
        }
    }

    private var nextPageJob: Job? = null

    fun nextPage() {
        if (nextPageJob != null) {
            return
        }
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

sealed interface HomeEvent

data class HomeState(
    val data: List<Manga> = emptyList(),
    val loadingNextPage: Boolean = false,
    val chapterImages: ChapterImages? = null
)

class HomeScreen: Screen {

    @Composable
    override fun Content() {

        val sm = getScreenModel<HomeSM>()

        val state by sm.state.collectAsState()

        val ctx = LocalContext.current

        val gridState = rememberLazyGridState()

        val lifecycleOwner = LocalLifecycleOwner.current

        LaunchedEffect(Unit) {
            snapshotFlow { gridState.firstVisibleItemIndex }.collect {
                if (it == gridState.layoutInfo.totalItemsCount - gridState.layoutInfo.visibleItemsInfo.size) {
                    sm.nextPage()
                }
            }
        }

        state.chapterImages?.let {
            Popup(alignment = Alignment.Center) {
                LazyRow {
                    items(it.images) { img ->
                        AsyncImage(
                            model = ImageRequest.Builder(ctx)
                                .data(img.uri)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.size(420.dp)
                        )
                    }
                }
            }
        }

        Scaffold { paddingValues ->
            Column(Modifier.padding(paddingValues)) {
                LazyVerticalGrid(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    state = gridState,
                    columns = GridCells.Fixed(2)
                ) {
                    items(
                        items = state.data,
                        key = { m: Manga -> m.id }
                    ) { manga ->
                        Column(
                            Modifier.clickable {
                                sm.loadImages(manga.id, lifecycleOwner)
                            }
                        ) {
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
                                    val s = if (manga.description.length <= 15)
                                        manga.description
                                    else
                                        manga.description.split(" ").take(15).joinToString()
                                    "$s..."
                                }
                            }

                            Text(
                                text = mangaText
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
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

