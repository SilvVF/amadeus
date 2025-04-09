package io.silv.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.silv.database.entity.chapter.ChapterEntity
import io.silv.database.entity.manga.MangaEntityWithChapters
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {

    @Insert
    suspend fun insertChapter(chapter: ChapterEntity)

    @Transaction
    suspend fun updateOrInsert(chapter: ChapterEntity) {
        // First check if the chapter exists
        val existingChapter = getChapterById(chapter.id)
        if (existingChapter != null) {
            updateChapter(
                chapter.copy(
                    lastPageRead = existingChapter.lastPageRead,
                    bookmarked = existingChapter.bookmarked,
                    createdAt = existingChapter.createdAt,
                )
            )  // Update the existing chapter
        } else {
            insertChapter(chapter)  // Insert a new chapter if it doesn't exist
        }
    }

    @Query("SELECT * FROM chapters WHERE id = :id LIMIT 1")
    fun observeChapterById(id: String): Flow<ChapterEntity?>

    @Query("SELECT * FROM chapters WHERE bookmarked")
    fun observeBookmarkedChapters(): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE id = :id LIMIT 1")
    suspend fun getChapterById(id: String): ChapterEntity?

    @Query(
        """
        SELECT * FROM chapters
        WHERE manga_id = :mangaId
    """,
    )
    fun observeChaptersByMangaId(mangaId: String): Flow<List<ChapterEntity>>

    @Query(
        """
        SELECT * FROM chapters 
        WHERE manga_id = :mangaId
    """,
    )
    suspend fun getChaptersByMangaId(mangaId: String): List<ChapterEntity>

    @Query("SELECT * FROM chapters")
    fun getChapterEntities(): Flow<List<ChapterEntity>>

    @Delete
    suspend fun deleteChapter(chapterEntity: ChapterEntity)

    @Update
    suspend fun updateChapter(chapterEntity: ChapterEntity)
}
