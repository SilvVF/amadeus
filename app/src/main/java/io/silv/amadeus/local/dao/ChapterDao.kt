package io.silv.amadeus.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.silv.amadeus.local.entity.ChapterEntity

@Dao
interface ChapterDao {

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun upsertChapter(chapterEntity: ChapterEntity)

    @Query("""
        SELECT * FROM chapterentity 
        WHERE cid = :id
        LIMIT 1
    """)
    suspend fun getChapterById(id: String): ChapterEntity?

    @Query("""
        SELECT * FROM chapterentity
    """)
    suspend fun getAll(): List<ChapterEntity>

    @Delete
    suspend fun deleteChapter(chapterEntity: ChapterEntity)
}