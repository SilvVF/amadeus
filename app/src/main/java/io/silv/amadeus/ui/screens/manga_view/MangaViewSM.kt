package io.silv.amadeus.ui.screens.manga_view

import androidx.compose.runtime.Immutable
import androidx.lifecycle.LiveData
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import cafe.adriel.voyager.core.model.coroutineScope
import com.skydoves.whatif.whatIfNotNull
import io.silv.amadeus.domain.models.DomainChapter
import io.silv.amadeus.domain.models.DomainCoverArt
import io.silv.amadeus.domain.repos.MangaRepo
import io.silv.amadeus.filterUnique
import io.silv.amadeus.local.workers.ChapterDownloadWorker
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.ktor_response_mapper.message
import io.silv.ktor_response_mapper.suspendOnFailure
import io.silv.ktor_response_mapper.suspendOnSuccess
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class MangaViewSM(
    private val mangaRepo: MangaRepo,
    private val workManager: WorkManager,
): AmadeusScreenModel<MangaViewEvent, MangaViewState>(MangaViewState()) {

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

    fun downloadChapter(
        chapter: DomainChapter,
    ) = coroutineScope.launch {
        val id = UUID.randomUUID()
        val request = OneTimeWorkRequestBuilder<ChapterDownloadWorker>()
            .setId(id)
            .setInputData(
                Data.Builder()
                    .putBoolean(ChapterDownloadWorker.fetchImagesKey, true)
                    .putString(ChapterDownloadWorker.volumeNumberKey, chapter.volume ?: "0")
                    .putString(ChapterDownloadWorker.chapterIdKey, chapter.id.also { println("chapter id ${chapter.id}") })
                    .putString(ChapterDownloadWorker.mangaIdKey, chapter.mangaId)
                    .build()
            )
            .build()
        workManager.enqueue(request)
        workManager.getWorkInfoByIdLiveData(id).whatIfNotNull {
            mutableEvents.send(
                MangaViewEvent.DownloadStart(chapter.id, it)
            )
        }
    }
}

sealed interface MangaViewEvent {
    data class DownloadStart(
        val chapterId: String,
        val observable: LiveData<WorkInfo>): MangaViewEvent
}

sealed class ChapterListState(
    open val chapters: List<DomainChapter> = emptyList(),
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
    val chapterListState: ChapterListState = ChapterListState.Loading,
)
