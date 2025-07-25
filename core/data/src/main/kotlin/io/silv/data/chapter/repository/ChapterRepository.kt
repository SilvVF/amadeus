package io.silv.data.chapter.repository

import io.silv.data.chapter.Chapter
import kotlinx.coroutines.flow.Flow

/**
 * Repository responsible for handling Chapters that have been saved to the local db.
 */
interface ChapterRepository {

    suspend fun bookmarkChapter(id: String)

    suspend fun updateChapter(id: String, copy: (Chapter) -> Chapter): Chapter?

    suspend fun updateLastReadPage(chapterId: String, page: Int, lastPage: Int)

    suspend fun saveChapters(ids: List<String>): Boolean

    suspend fun saveChapter(chapter: Chapter)

    suspend fun refetchChapter(id: String): Chapter?

    suspend fun refetchChapters(mangaId: String)

    suspend fun getChaptersByMangaId(mangaId: String): List<Chapter>

    fun observeChaptersByMangaId(mangaId: String): Flow<List<Chapter>>

    suspend fun getChapterById(id: String): Chapter?

    fun observeChapterById(id: String): Flow<Chapter>

    fun observeChapters(): Flow<List<Chapter>>

    fun observeBookmarkedChapters(): Flow<List<Chapter>>
}

