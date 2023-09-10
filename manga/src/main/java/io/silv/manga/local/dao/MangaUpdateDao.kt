package io.silv.manga.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.silv.manga.local.entity.MangaUpdateEntity
import io.silv.manga.local.entity.relations.MangaUpdateEntityWithManga
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaUpdateDao {

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun upsert(mangaUpdateEntity: MangaUpdateEntity)

    @Delete
    suspend fun delete(mangaUpdateEntity: MangaUpdateEntity)

    @Query("SELECT * FROM MangaUpdateEntity")
    fun getAllUpdates(): List<MangaUpdateEntity>

    @Transaction
    @Query("SELECT * FROM MangaUpdateEntity")
    fun observeAllUpdatesWithManga(): Flow<List<MangaUpdateEntityWithManga>>
}