package io.silv.manga.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.silv.manga.local.entity.MangaEntity
import io.silv.manga.local.relations.MangaWithChapters
import io.silv.manga.sync.SyncableDao

@Dao
interface MangaDao: SyncableDao<MangaEntity> {

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun upsertManga(mangaEntity: MangaEntity)


    @Query("SELECT * FROM mangaentity")
    suspend fun getAll(): List<MangaEntity>

    @Query("""
       SELECT * FROM mangaentity
       WHERE id = :id
       LIMIT 1
    """)
    suspend fun getMangaById(id: String):  MangaEntity?

    @Transaction
    @Query("""
      SELECT * FROM mangaentity
      WHERE id = :id
      LIMIT 1
    """)
    suspend fun getMangaWithChapters(id: String): MangaWithChapters?

    @Delete
    suspend fun delete(mangaEntity: MangaEntity)
}