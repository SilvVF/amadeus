package io.silv.domain.manga

import io.silv.data.chapter.ChapterRepository
import io.silv.data.download.DownloadManager
import io.silv.data.manga.SavedMangaRepository
import io.silv.data.manga.SourceMangaRepository
import io.silv.model.SavableChapter
import io.silv.model.SavableManga
import io.silv.model.SavableMangaWithChapters
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate

/**
 * Combines Saved manga with all resource repository's and transforms the manga received by id into a
 * flow of [SavableMangaWithChapters]. also observes the chapter cache and
 * refreshes on changes to download status.
 */
class GetCombinedSavableMangaWithChapters(
    private val sourceMangaRepository: SourceMangaRepository,
    private val savedMangaRepository: SavedMangaRepository,
    private val chapterInfoRepository: ChapterRepository,
    private val downloadManager: DownloadManager,
) {
    fun subscribe(id: String): Flow<SavableMangaWithChapters> {
        return combine(
            sourceMangaRepository.observeMangaById(id),
            savedMangaRepository.observeSavedMangaById(id),
            chapterInfoRepository.getChapters(id),
            downloadManager.cacheChanges,
        ) { source, saved, chapterInfo, cacheChange ->
            SavableMangaWithChapters(
                savableManga = saved?.let(::SavableManga) ?: source!!.let(::SavableManga),
                chapters =
                chapterInfo.map(::SavableChapter).map {
                    it.copy(
                        downloaded = downloadManager.isChapterDownloaded(
                            it.title,
                            it.scanlator,
                            saved?.title ?: source!!.title
                        ),
                    )
                }.toImmutableList(),
            )
        }
            .conflate()
    }
}
