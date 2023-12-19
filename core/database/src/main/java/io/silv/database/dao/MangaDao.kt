package io.silv.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.silv.database.entity.manga.MangaEntity
import io.silv.database.entity.relations.MangaEntityWithChapters
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaDao {
    @Update
    suspend fun update(manga: MangaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(manga: MangaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mangas: List<MangaEntity>)

    @Delete
    suspend fun delete(manga: MangaEntity)

    @Query("SELECT * FROM MANGA WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MangaEntity?

    @Query("SELECT * FROM MANGA WHERE id in (:ids)")
    suspend fun getByIds(ids: List<String>): List<MangaEntity>

    @Query("SELECT * FROM MANGA WHERE id in (:ids)")
    fun observeByIds(ids: List<String>): Flow<List<MangaEntity>>

    @Query("SELECT * FROM MANGA WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<MangaEntity?>

    @Query("SELECT * FROM MANGA")
    suspend fun getAll(): List<MangaEntity>

    @Query("SELECT * FROM MANGA")
    fun observeAll(): Flow<List<MangaEntity>>

    @Query("SELECT * FROM MANGA WHERE favorite")
    suspend fun getLibraryManga(): List<MangaEntity>

    @Query("SELECT * FROM MANGA WHERE favorite")
    fun observeLibraryManga(): Flow<List<MangaEntity>>

    @Transaction
    @Query("SELECT * FROM MANGA WHERE favorite")
    fun observeLibraryMangaWithChapters(): Flow<List<MangaEntityWithChapters>>

    @Transaction
    @Query("SELECT * FROM MANGA WHERE id = :id")
    fun observeMangaWithChaptersById(id: String): Flow<MangaEntityWithChapters>
}
