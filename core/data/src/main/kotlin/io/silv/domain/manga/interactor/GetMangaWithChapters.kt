package io.silv.domain.manga.interactor

import io.silv.domain.chapter.model.Chapter
import io.silv.domain.chapter.repository.ChapterRepository
import io.silv.domain.manga.model.MangaWithChapters
import io.silv.domain.manga.repository.MangaRepository
import kotlinx.coroutines.flow.Flow

/**
 * Combines Saved manga with all resource repository's and transforms the manga received by id into a
 * flow of [MangaWithChapters]. also observes the chapter cache and
 * refreshes on changes to download status.
 */
class GetMangaWithChapters(
    private val mangaRepository: MangaRepository,
) {
    fun subscribe(id: String): Flow<MangaWithChapters> {
        return mangaRepository.observeMangaWithChaptersById(id)
    }

    suspend fun await(id: String): MangaWithChapters? {
        return mangaRepository.getMangaWithChaptersById(id)
    }
}

class GetChaptersByMangaId(
    private val chapterRepository: ChapterRepository,
) {
    fun subscribe(id: String): Flow<List<Chapter>> {
        return chapterRepository.observeChaptersByMangaId(id)
    }

    suspend fun await(id: String): List<Chapter> {
        return chapterRepository.getChaptersByMangaId(id)
    }
}
