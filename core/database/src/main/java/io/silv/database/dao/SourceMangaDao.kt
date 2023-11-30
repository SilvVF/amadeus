package io.silv.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.silv.database.entity.manga.SourceMangaResource
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceMangaDao {

    @Update
    suspend fun update(manga: SourceMangaResource)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(manga: SourceMangaResource)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mangas: List<SourceMangaResource>)

    @Delete
    suspend fun delete(manga: SourceMangaResource)

    @Query("SELECT * FROM SourceMangaResource WHERE id = :id LIMIT 1")
    suspend fun selectById(id: String): SourceMangaResource?

    @Query("SELECT * FROM SourceMangaResource WHERE id in (:ids)")
    fun observeByIds(ids: List<String>): Flow<List<SourceMangaResource>>

    @Query("SELECT * FROM SourceMangaResource WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<SourceMangaResource?>

    @Query("SELECT * FROM SourceMangaResource")
    suspend fun selectAll(): List<SourceMangaResource>

    @Query("SELECT * FROM SourceMangaResource")
    fun observeAll(): Flow<List<SourceMangaResource>>
}