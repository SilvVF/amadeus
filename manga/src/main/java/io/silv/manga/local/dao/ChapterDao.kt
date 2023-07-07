package io.silv.manga.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.sync.SyncableDao
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ChapterDao: SyncableDao<ChapterEntity> {

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun upsertChapter(chapterEntity: ChapterEntity)

    @Query("SELECT * FROM chapterentity WHERE id = :id")
    suspend fun getById(id: String): ChapterEntity?

    @Query("""
        SELECT * FROM chapterentity 
        WHERE manga_id = :mangaId
    """)
    fun getChaptersByMangaId(mangaId: String): Flow<List<ChapterEntity>>

    @Query("""
        SELECT * FROM chapterentity
    """)
    suspend fun getAll(): List<ChapterEntity>

    @Delete
    suspend fun deleteChapter(chapterEntity: ChapterEntity)
}