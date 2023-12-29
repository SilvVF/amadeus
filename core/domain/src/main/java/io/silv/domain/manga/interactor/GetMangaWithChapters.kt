package io.silv.domain.manga.interactor

import io.silv.domain.chapter.repository.ChapterRepository
import io.silv.domain.manga.model.MangaWithChapters
import io.silv.domain.manga.repository.MangaRepository
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull

/**
 * Combines Saved manga with all resource repository's and transforms the manga received by id into a
 * flow of [MangaWithChapters]. also observes the chapter cache and
 * refreshes on changes to download status.
 */
class GetMangaWithChapters(
    private val mangaRepository: MangaRepository,
    private val chapterInfoRepository: ChapterRepository,
) {
    fun subscribe(id: String): Flow<MangaWithChapters> {
        return combine(
            mangaRepository.observeMangaById(id).filterNotNull(),
            chapterInfoRepository.observeChaptersByMangaId(id),
        ) { manga, chapterInfo ->
            MangaWithChapters(
                manga = manga,
                chapters = chapterInfo.toImmutableList()
            )
        }
            .conflate()
    }

    suspend fun await(id: String): MangaWithChapters? = subscribe(id).firstOrNull()
}
