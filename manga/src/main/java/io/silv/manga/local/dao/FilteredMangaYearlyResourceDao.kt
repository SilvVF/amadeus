package io.silv.manga.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.silv.manga.local.entity.FilteredMangaYearlyResource
import kotlinx.coroutines.flow.Flow

@Dao
internal interface FilteredMangaYearlyResourceDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertManga(mangaResource: FilteredMangaYearlyResource)


    @Query("""
          SELECT * FROM FilteredMangaYearlyResource
          ORDER BY savedLocalAtEpochSeconds ASC
    """)
    fun getMangaResources(): Flow<List<FilteredMangaYearlyResource>>

    @Query("SELECT * FROM FilteredMangaYearlyResource WHERE id = :mangaId")
    fun getResourceAsFlowById(mangaId: String): Flow<FilteredMangaYearlyResource?>

    @Query("SELECT * FROM FilteredMangaYearlyResource")
    suspend fun getAll(): List<FilteredMangaYearlyResource>

    @Update
    suspend fun update(mangaResource: FilteredMangaYearlyResource)


    @Query("SELECT * FROM FilteredMangaYearlyResource WHERE id = :id LIMIT 1")
    suspend fun getMangaById(id: String):  FilteredMangaYearlyResource?


    @Delete
    suspend fun delete(mangaResource: FilteredMangaYearlyResource)

    companion object {
        const val id = 13
    }
}