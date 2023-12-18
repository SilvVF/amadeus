package io.silv.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.silv.database.entity.manga.SavedMangaEntity
import io.silv.database.entity.relations.SavedMangaWithChapters
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedMangaDao : SyncableDao<SavedMangaEntity> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavedManga(mangaEntity: SavedMangaEntity)

    @Update
    fun updateSavedManga(mangaEntity: SavedMangaEntity)

    @Query("SELECT * FROM savedmangaentity")
    fun getSavedManga(): Flow<List<SavedMangaEntity>>

    @Query("SELECT * FROM savedmangaentity WHERE id = :id")
    fun getSavedMangaById(id: String): Flow<SavedMangaEntity?>

    @Transaction
    @Query("SELECT * FROM savedmangaentity WHERE id = :id")
    fun getSavedMangaWithChaptersById(id: String): Flow<SavedMangaWithChapters?>

    @Transaction
    @Query("SELECT * FROM savedmangaentity")
    fun getSavedMangaWithChapters(): Flow<List<SavedMangaWithChapters>>

    @Delete
    suspend fun deleteSavedManga(mangaEntity: SavedMangaEntity)

    companion object {
        const val id: Int = 7
    }
}
