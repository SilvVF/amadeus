package io.silv.manga.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.silv.manga.local.entity.FilteredMangaResource
import kotlinx.coroutines.flow.Flow

@Dao
internal interface FilteredMangaResourceDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertManga(mangaResource: FilteredMangaResource)


    @Query("""
          SELECT * FROM FilteredMangaResource
          ORDER BY savedLocalAtEpochSeconds ASC
    """)
    fun getMangaResources(): Flow<List<FilteredMangaResource>>

    @Query("SELECT * FROM FilteredMangaResource WHERE id = :mangaId")
    fun getResourceAsFlowById(mangaId: String): Flow<FilteredMangaResource?>

    @Query("SELECT * FROM FilteredMangaResource")
    suspend fun getAll(): List<FilteredMangaResource>

    @Update
    suspend fun update(mangaResource: FilteredMangaResource)


    @Query("SELECT * FROM FilteredMangaResource WHERE id = :id LIMIT 1")
    suspend fun getMangaById(id: String):  FilteredMangaResource?


    @Delete
    suspend fun delete(mangaResource: FilteredMangaResource)

    companion object {
        const val id = 19
    }
}
