package io.silv.amadeus.ui.screens.manga_reader

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import cafe.adriel.voyager.core.model.coroutineScope
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.core.combineTuple
import io.silv.manga.domain.models.SavableChapter
import io.silv.manga.domain.models.SavableManga
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.domain.repositorys.base.Resource
import io.silv.manga.domain.repositorys.chapter.ChapterImageRepository
import io.silv.manga.domain.usecase.GetCombinedSavableMangaWithChapters
import io.silv.manga.domain.usecase.GetMangaAggregate
import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.local.workers.ChapterDownloadWorker
import io.silv.manga.network.mangadex.models.chapter.Chapter
import io.silv.manga.network.mangadex.models.manga.MangaAggregateResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MangaReaderSM(
    private val workManager: WorkManager,
    private val savedMangaRepository: SavedMangaRepository,
    getCombinedSavableMangaWithChapters: GetCombinedSavableMangaWithChapters,
    private val chapterImageRepository: ChapterImageRepository,
    getMangaAggregate: GetMangaAggregate,
    private val mangaId: String,
    initialChapterId: String,
): AmadeusScreenModel<MangaReaderEvent>() {

    private val chapterId = MutableStateFlow(initialChapterId)

    val mangaReaderState = combineTuple(
        chapterId,
        getCombinedSavableMangaWithChapters(mangaId),
        getMangaAggregate(mangaId)
    ).map { (chapterId, savableWithChapters, aggregateResource) ->
            val (manga, chapters) = savableWithChapters
            if (manga == null) {
                return@map MangaReaderState.Failure("manga not found")
            }
            val chapter = chapters.find { c -> c.id == chapterId } ?: return@map MangaReaderState.Failure("chapter not found")
            if (chapter.downloaded) {
                MangaReaderState.Success(
                    manga = manga,
                    chapter = SavableChapter(chapter),
                    chapters = when(aggregateResource) {
                        is Resource.Failure, Resource.Loading -> chapters.map { SavableChapter(it) }
                        is Resource.Success -> combineAggregateAndChapter(
                            aggregateResource.result,
                            chapter,
                            chapters
                        )
                            .values.flatten()
                    },
                    pages = chapter.chapterImages
                )
            } else {
                chapterImageRepository.getChapterImages(chapterId)
                    .fold<Resource<Pair<Chapter, List<String>>>, MangaReaderState>(MangaReaderState.Loading) { initial, resource ->
                    when (resource) {
                        is Resource.Failure -> MangaReaderState.Failure(resource.message)
                        Resource.Loading -> MangaReaderState.Loading
                        is Resource.Success -> MangaReaderState.Success(
                            manga = manga,
                            chapter = SavableChapter(chapter),
                            chapters = chapters.map { SavableChapter(it) },
                            pages = resource.result.second
                        )
                    }
                }
            }
    }
        .stateInUi(MangaReaderState.Loading)


    fun goToNextChapter(chapter: SavableChapter) = coroutineScope.launch {
        chapterId.update { id ->
            val chapterNumber = chapter.chapter
            mangaReaderState.value.success?.let { state ->
                state.chapters.find {
                    it.chapter == chapterNumber + 1
                }?.id ?: id
            } ?: id
        }
    }

    fun goToPrevChapter(chapter: SavableChapter) = coroutineScope.launch {
        chapterId.update { id ->
            val chapterNumber = chapter.chapter ?: 0
            mangaReaderState.value.success?.let { state ->
                state.chapters.find {
                    it.chapter == chapterNumber - 1
                }?.id ?: id
            } ?: id
        }
    }

    fun goToChapter(id: String) = coroutineScope.launch {
        chapterId.emit(id)
    }

    fun updateChapterPage(page: Int) = coroutineScope.launch {
        savedMangaRepository.updateLastReadPage(mangaId, chapterId.value, page)
    }

    private fun downloadMangaImages() = coroutineScope.launch {
        workManager.enqueueUniqueWork(
            chapterId.value,
            ExistingWorkPolicy.KEEP,
            ChapterDownloadWorker.downloadWorkRequest(
                listOf(chapterId.value),
                mangaId
            )
        )
    }

}

private fun combineAggregateAndChapter(
    aggregateResponse: MangaAggregateResponse,
    viewingChapter: ChapterEntity,
    savedChapters: List<ChapterEntity>
): Map<Int, List<SavableChapter>> {
    val volumeToChapters = aggregateResponse.volumes
        .mapKeys { (volumeNumber, _) -> volumeNumber.toIntOrNull() ?: -1 }
        .mapValues { (_, volume) ->
            return@mapValues buildList {
                volume.chapters.forEach { (num, chapter) ->
                    val chapterList = (chapter.others + chapter.chapter)
                    if (viewingChapter.id in chapterList) {
                        add(SavableChapter(viewingChapter))
                    } else {
                        val prioChapter = savedChapters.find { chapter.chapter == it.id }
                        if (prioChapter != null) {
                            add(SavableChapter(prioChapter))
                        } else {
                            savedChapters.firstOrNull { it.id in chapter.others }
                                ?.let { add(SavableChapter(it)) }
                        }
                    }
                }
            }
        }
    return volumeToChapters
}

fun <T, V> Flow<T>.combineToPair(other: Flow<V>): Flow<Pair<T, V>> = this.combine(other) { t, v -> Pair(t, v) }

sealed interface MangaReaderEvent

sealed class MangaReaderState {

    object Loading: MangaReaderState()

    data class Success(
        val manga: SavableManga,
        val chapter: SavableChapter,
        val chapters: List<SavableChapter>,
        val pages: List<String> = emptyList()
    ): MangaReaderState()

    data class Failure(val message: String? = null): MangaReaderState()

    val success: Success?
        get() = this as? Success
}