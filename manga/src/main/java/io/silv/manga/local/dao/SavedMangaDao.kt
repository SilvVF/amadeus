package io.silv.manga.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.silv.manga.local.entity.SavedMangaEntity
import io.silv.manga.local.entity.relations.MangaWithChapters
import io.silv.manga.sync.SyncableDao
import kotlinx.coroutines.flow.Flow

@Dao
internal interface SavedMangaDao: SyncableDao<SavedMangaEntity> {

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun upsertManga(mangaEntity: SavedMangaEntity)


    @Query("SELECT * FROM savedmangaentity")
    fun getAllAsFlow(): Flow<List<SavedMangaEntity>>

    @Query("SELECT * FROM savedmangaentity")
    suspend fun getAll(): List<SavedMangaEntity>



    @Query("""
       SELECT * FROM savedmangaentity
       WHERE id = :id
       LIMIT 1
    """)
    suspend fun getMangaById(id: String):  SavedMangaEntity?

    @Transaction
    @Query("""
      SELECT * FROM savedmangaentity
      WHERE id = :id
      LIMIT 1
    """)
    suspend fun getMangaWithChapters(id: String): MangaWithChapters?

    @Delete
    suspend fun delete(mangaEntity: SavedMangaEntity)
}