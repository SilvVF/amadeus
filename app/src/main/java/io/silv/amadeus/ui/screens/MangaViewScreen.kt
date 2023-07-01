package io.silv.amadeus.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.coroutineScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import io.silv.amadeus.domain.models.DomainChapter
import io.silv.amadeus.domain.models.DomainCoverArt
import io.silv.amadeus.domain.models.DomainManga
import io.silv.amadeus.domain.repos.MangaRepo
import io.silv.amadeus.domain.repos.toDomainChapter
import io.silv.amadeus.network.mangadex.MangaDexTestApi
import io.silv.amadeus.network.mangadex.requests.OrderBy
import io.silv.amadeus.ui.composables.AnimatedBoxShimmer
import io.silv.amadeus.ui.composables.AnimatedShimmer
import io.silv.amadeus.ui.composables.MangaViewPoster
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.amadeus.ui.shared.CenterBox
import io.silv.amadeus.ui.stateholders.VolumeItemsState
import io.silv.amadeus.ui.stateholders.rememberVolumeItemsState
import io.silv.amadeus.ui.theme.LocalSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.annotations.Async

class MangaViewSM(
    private val mangaRepo: MangaRepo,
    mangaDexTestApi: MangaDexTestApi,
): AmadeusScreenModel<MangaViewEvent, MangaViewState>(MangaViewState()) {

    private var mangaFeedOffset: Int = 0

    init {
        coroutineScope.launch {
            delay(3000)
            mutableState.value = MangaViewState(
                chapterListState = ChapterListState.Success(
                    mangaDexTestApi.getChapterList().data.map(::toDomainChapter)
                ),
                coverArtState = CoverArtState.Success(
                    buildMap {
                        mangaDexTestApi.getMangaCoverArt().data.forEach {
                            put(it.attributes.volume,
                                DomainCoverArt(
                                volume = it.attributes.volume,
                                mangaId = "a93959d7-4a4a-4f80-88f7-921af3ca9ade",
                                coverArtUrl = "https://uploads.mangadex.org/covers/a93959d7-4a4a-4f80-88f7-921af3ca9ade/${it.attributes.fileName}"
                            ))
                        }
                    }
                )
            )
        }
    }

    fun loadMangaInfo(
        mangaId: String,
        orderBy: OrderBy = OrderBy.asc,
        resetOffset: Boolean = false
    ) = coroutineScope.launch {

    }
}

sealed interface MangaViewEvent

sealed class ChapterListState(
    open val chapters: List<DomainChapter> = emptyList()
) {
    object Loading: ChapterListState()
    data class Success(override val chapters: List<DomainChapter>): ChapterListState(chapters)
    data class Failure(val message: String): ChapterListState()
}

sealed class CoverArtState(
    open val art: Map<String?, DomainCoverArt> = emptyMap()
) {
    object Loading: CoverArtState()
    data class Success(override val art: Map<String?, DomainCoverArt>): CoverArtState(art)
    data class Failure(val message: String): CoverArtState()
}

@Immutable
data class MangaViewState(
    val coverArtState: CoverArtState = CoverArtState.Loading,
    val chapterListState: ChapterListState = ChapterListState.Loading
)

class MangaViewScreen(
  val manga: DomainManga
): Screen {

    @Composable
    override fun Content() {

        val sm = getScreenModel<MangaViewSM>()

        val mangaViewState by sm.state.collectAsStateWithLifecycle()

        val ctx = LocalContext.current
        val space = LocalSpacing.current

        LaunchedEffect(Unit) {
            sm.loadMangaInfo(manga.id)
        }

        Scaffold { paddingValues ->
            Column(
                Modifier.padding(paddingValues)
            ) {
               MangaViewPoster(
                   modifier = Modifier
                       .fillMaxHeight(0.4f)
                       .fillMaxWidth(),
                   manga = manga,
                   onReadNowClick = {},
                   onBookMarkClick = {}
               )
               MangaChapterList(
                   state = mangaViewState.chapterListState,
                   coverArtState = mangaViewState.coverArtState
               )
            }
        }
    }
}


@Composable
fun MangaChapterList(
    state: ChapterListState,
    coverArtState: CoverArtState
) {
    when(state) {
        is ChapterListState.Failure -> {
            CenterBox(Modifier.fillMaxSize()) {
                Text(state.message)
            }
        }
        ChapterListState.Loading -> {
            Column(Modifier.fillMaxSize()) {
                repeat(5) {
                    AnimatedShimmer()
                }
            }
        }
        is ChapterListState.Success -> {

            val volumeItemsState = rememberVolumeItemsState(chapters = state.chapters)
            Row {
                Button(
                    onClick = {
                        volumeItemsState.groupBy(VolumeItemsState.GroupBy.Volume)
                    }
                ){}
                Button(
                    onClick = {
                        volumeItemsState.groupBy(VolumeItemsState.GroupBy.Chapter)
                    }
                ){}
                Button(
                    onClick = {
                        volumeItemsState.sortBy(VolumeItemsState.SortBy.Asc)
                    }
                ){}
                Button(
                    onClick = {
                        volumeItemsState.sortBy(VolumeItemsState.SortBy.Dsc)
                    }
                ){}
            }

            when (volumeItemsState.items) {
                is VolumeItemsState.Chapters -> {
                    rememberLazyListState()
                    ChapterList(volumeItemsState.items)
                }
                is VolumeItemsState.Volumes -> {
                    VolumeList(volumeItemsState.items, coverArtState)
                }
            }
        }
    }
}

@Composable
private fun VolumeList(
    volumeItems: VolumeItemsState.VolumeItems,
    coverArtState: CoverArtState
) {

    val ctx = LocalContext.current

    when (volumeItems) {
        is VolumeItemsState.Volumes -> {
            LazyColumn(Modifier.fillMaxSize()) {
                items(
                    volumeItems.items,
                    key = { items -> items.first().volume ?: "0" }
                ) { chapters ->

                    var expanded by rememberSaveable { mutableStateOf(false) }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text =  chapters.first().volume ?: ""
                            )
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(
                                    imageVector = if (expanded)
                                        Icons.Filled.KeyboardArrowUp
                                    else
                                        Icons.Filled.KeyboardArrowDown,
                                    contentDescription = null
                                )
                            }
                        }
                        when (coverArtState) {
                            is CoverArtState.Failure -> {

                            }
                            CoverArtState.Loading -> {

                            }
                            is CoverArtState.Success -> {
                                SubcomposeAsyncImage(
                                    model = ImageRequest.Builder(ctx)
                                        .data(
                                            coverArtState.art[chapters.first().volume]?.coverArtUrl
                                        )
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    loading = {
                                        AnimatedBoxShimmer()
                                    },
                                    error = {
                                        AnimatedBoxShimmer()
                                    },
                                    modifier = Modifier.size(120.dp)
                                )
                            }
                        }
                        if (expanded) {
                            for (chapter in chapters) {
                                Text(text = chapter.title ?: "")
                            }
                        }
                    }
                }
            }
         }
        else -> Unit
    }
}


@Composable
private fun ChapterList(
    volumeItems: VolumeItemsState.VolumeItems,
) {
    when (volumeItems) {
        is VolumeItemsState.Chapters -> {
            LazyColumn(Modifier.fillMaxSize()) {
                items(
                    volumeItems.items,
                ) { chapter ->
                    Text(text = chapter.title ?: "")
                }
            }
        }
        else -> Unit
    }
}
