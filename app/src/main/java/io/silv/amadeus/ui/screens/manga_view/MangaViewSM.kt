package io.silv.amadeus.ui.screens.manga_view

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.coroutineScope
import io.silv.amadeus.domain.models.DomainChapter
import io.silv.amadeus.domain.models.DomainCoverArt
import io.silv.amadeus.domain.repos.MangaRepo
import io.silv.amadeus.filterUnique
import io.silv.amadeus.network.mangadex.MangaDexTestApi
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.ktor_response_mapper.message
import io.silv.ktor_response_mapper.suspendOnFailure
import io.silv.ktor_response_mapper.suspendOnSuccess
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MangaViewSM(
    private val mangaRepo: MangaRepo,
    mangaDexTestApi: MangaDexTestApi,
): AmadeusScreenModel<MangaViewEvent, MangaViewState>(MangaViewState()) {

    init {
//        coroutineScope.launch {
//            delay(3000)
//            mutableState.value = MangaViewState(
//                chapterListState = ChapterListState.Success(
//                    mangaDexTestApi.getChapterList().data.map(::toDomainChapter)
//                ),
//                coverArtState = CoverArtState.Success(
//                    buildMap {
//                        mangaDexTestApi.getMangaCoverArt(
//
//                        ).data.forEach {
//                            put(it.attributes.volume,
//                                DomainCoverArt(
//                                volume = it.attributes.volume,
//                                mangaId = "a93959d7-4a4a-4f80-88f7-921af3ca9ade",
//                                coverArtUrl = "https://uploads.mangadex.org/covers/a93959d7-4a4a-4f80-88f7-921af3ca9ade/${it.attributes.fileName}"
//                            ))
//                        }
//                    }
//                )
//            )
//        }
    }

    fun loadVolumeCoverArt(
        mangaId: String,
        volumeCount: Int,
        offset: Int = 0
    ) = coroutineScope.launch {
        mangaRepo.getVolumeImages(mangaId, 100, offset)
            .suspendOnSuccess {
                mutableState.update { state ->
                    state.copy(
                        coverArtState = CoverArtState.Success(
                            art = buildMap {
                                state.coverArtState.art.forEach { put(it.key, it.value) }
                                data.forEach { put(it.volume, it) }
                            }
                        )
                    )
                }
            }
            .suspendOnFailure {
                mutableState.update { state ->
                    state.copy(
                        coverArtState = CoverArtState.Failure(message())
                    )
                }
            }
    }

    fun loadMangaInfo(
        mangaId: String,
        chapterCount: Int,
    ) = coroutineScope.launch {
        mangaRepo.getMangaFeed(mangaId, limit = 500)
            .suspendOnSuccess {
                mutableState.update { state ->
                    state.copy(
                        chapterListState = ChapterListState.Success(
                            chapters = data.filterUnique { it.chapter }
                                .filter { it.chapter != null }
                        )
                    )
                }
            }
            .suspendOnFailure {
                mutableState.update { state ->
                    state.copy(
                        chapterListState = ChapterListState.Failure(message())
                    )
                }
            }
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
