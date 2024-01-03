package io.silv.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.silv.database.entity.manga.MangaUpdateEntity
import io.silv.database.entity.manga.MangaUpdateEntityWithManga
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaUpdateDao {
    @Insert(
        onConflict = OnConflictStrategy.REPLACE,
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
