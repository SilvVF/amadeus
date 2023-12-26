package io.silv.domain.manga

import io.silv.data.chapter.ChapterRepository
import io.silv.data.download.DownloadManager
import io.silv.data.manga.MangaRepository
import io.silv.model.SavableChapter
import io.silv.model.SavableManga
import io.silv.model.SavableMangaWithChapters
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull

/**
 * Combines Saved manga with all resource repository's and transforms the manga received by id into a
 * flow of [SavableMangaWithChapters]. also observes the chapter cache and
 * refreshes on changes to download status.
 */
class GetSavableMangaWithChapters(
    private val mangaRepository: MangaRepository,
    private val chapterInfoRepository: ChapterRepository,
    private val downloadManager: DownloadManager,
) {
    fun subscribe(id: String): Flow<SavableMangaWithChapters> {
        return combine(
            mangaRepository.observeMangaById(id).filterNotNull(),
            chapterInfoRepository.observeChaptersByMangaId(id),
            downloadManager.cacheChanges,
            downloadManager.queueState
        ) { manga, chapterInfo, _, _ ->
            SavableMangaWithChapters(
                savableManga = manga.let(::SavableManga),
                chapters =
                chapterInfo.map(::SavableChapter).map {
                    it.copy(
                        downloaded = downloadManager.isChapterDownloaded(
                            it.title,
                            it.scanlator,
                            manga.title
                        ),
                    )
                }.toImmutableList(),
            )
        }
            .conflate()
    }

    suspend fun await(id: String): SavableMangaWithChapters? = subscribe(id).firstOrNull()
}
