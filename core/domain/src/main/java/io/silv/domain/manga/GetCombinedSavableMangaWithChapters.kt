package io.silv.domain.manga

import eu.kanade.tachiyomi.reader.DownloadManager
import io.silv.data.chapter.ChapterRepository
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
 * flow of [SavableMangaWithChapters]. This contains all chapters with this mangaId as a foreign key.
 * If the manga is not found it will try to fetch it from the MangaDexApi and save it in memory.
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
            downloadManager.queueState
        ) { source, saved, chapterInfo, queueState ->
            SavableMangaWithChapters(
                savableManga = saved?.let(::SavableManga) ?: source!!.let(::SavableManga),
                chapters = chapterInfo.map(::SavableChapter).map {
                    it.copy(
                        downloaded = downloadManager.isChapterDownloaded(it.title, it.scanlator, saved?.titleEnglish ?: source!!.titleEnglish)
                    )
                }.toImmutableList(),
            )
        }
            .conflate()
    }
}






