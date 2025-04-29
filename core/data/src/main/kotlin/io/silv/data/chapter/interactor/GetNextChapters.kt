package io.silv.domain.chapter.interactor

import io.silv.domain.chapter.model.Chapter
import io.silv.data.manga.interactor.GetChaptersByMangaId
import io.silv.data.manga.interactor.GetManga
import kotlin.math.max


class GetNextChapters(
    private val getChaptersByMangaId: GetChaptersByMangaId,
    private val getManga: GetManga,
) {

    suspend fun await(mangaId: String): List<Chapter> {
        val manga = getManga.await(mangaId) ?: return emptyList()
        val chapters = getChaptersByMangaId.await(manga.id)
            .groupBy { it.scanlatorOrNull }
            .mapValues { (_, value) ->
                value.sortedBy { it.chapter }
            }
            .values
            .flatten()

        return chapters.filterNot { it.read }
    }

    suspend fun await(mangaId: String, fromChapterId: String): List<Chapter> {
        val chapters = await(mangaId)
        val currChapterIndex = chapters.indexOfFirst { it.id == fromChapterId }
        val nextChapters = chapters.subList(max(0, currChapterIndex), chapters.size)

        return nextChapters
    }
}