package io.silv.amadeus.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.coroutineScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import io.silv.amadeus.domain.models.DomainChapter
import io.silv.amadeus.domain.models.DomainManga
import io.silv.amadeus.domain.repos.MangaRepo
import io.silv.amadeus.domain.repos.toDomainChapter
import io.silv.amadeus.network.mangadex.MangaDexTestApi
import io.silv.amadeus.network.mangadex.requests.OrderBy
import io.silv.amadeus.ui.composables.AnimatedShimmer
import io.silv.amadeus.ui.composables.MangaViewPoster
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.ktor_response_mapper.suspendOnSuccess
import kotlinx.coroutines.launch

class MangaViewSM(
    private val mangaRepo: MangaRepo,
    mangaDexTestApi: MangaDexTestApi,
): AmadeusScreenModel<MangaViewEvent, MangaViewState>(MangaViewState.Loading) {

    private var mangaFeedOffset: Int = 0

    init {
        coroutineScope.launch {
            mutableState.value = MangaViewState.Success(
                mangaDexTestApi.getChapterList().data.map(::toDomainChapter)
            )
        }
    }

    fun loadMangaInfo(
        mangaId: String,
        orderBy: OrderBy = OrderBy.asc,
        resetOffset: Boolean = false
    ) = coroutineScope.launch {

        if (resetOffset) {
            mangaFeedOffset = 0
        }

        mangaRepo.getMangaFeed(mangaId, mangaFeedOffset, orderBy)
            .suspendOnSuccess {
                mutableState.emit(
                    MangaViewState.Success(
                        chapters = data
                    )
                )
            }
    }
}

sealed interface MangaViewEvent

@Immutable
sealed class MangaViewState(
    open val chapters: List<DomainChapter> = emptyList()
) {
    object Loading: MangaViewState()
    data class Success(override val chapters: List<DomainChapter>): MangaViewState(chapters)
    data class Failure(val message: String): MangaViewState()
}

class MangaViewScreen(
  val manga: DomainManga
): Screen {

    @Composable
    override fun Content() {

        val sm = getScreenModel<MangaViewSM>()

        val volumesState = sm.state.collectAsStateWithLifecycle()

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
                LazyColumn {
                    volumeItems(state = volumesState.value)
                }
            }
        }
    }
}

fun LazyListScope.volumeItems(
    state: MangaViewState
) {
    when (state) {
        is MangaViewState.Failure -> item {
            Text(text = state.message, color = MaterialTheme.colorScheme.error)
        }

        MangaViewState.Loading -> item {
            repeat(3) {
                AnimatedShimmer()
            }
        }

        is MangaViewState.Success -> items(
            items = state.chapters,
            key = { chapter -> chapter }
        ) { chapter ->
            Row {
                chapter.title?.let {
                    Text(text = it, style = MaterialTheme.typography.titleMedium)
                }
                Text(text = chapter.chapter ?: "")
            }
        }
    }
}