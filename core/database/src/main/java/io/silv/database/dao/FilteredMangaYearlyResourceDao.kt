package io.silv.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.silv.database.entity.manga.resource.FilteredMangaYearlyResource
import kotlinx.coroutines.flow.Flow

@Dao
interface FilteredMangaYearlyResourceDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFilteredYearlyMangaResource(mangaResource: FilteredMangaYearlyResource)


    @Query("SELECT * FROM FilteredMangaYearlyResource")
    fun getFilteredMangaYearlyResources(): Flow<List<FilteredMangaYearlyResource>>

    @Query("SELECT * FROM FilteredMangaYearlyResource WHERE id = :mangaId")
    fun observeFilteredYearlyMangaResourceById(mangaId: String): Flow<FilteredMangaYearlyResource?>

    @Update
    suspend fun updateFilteredYearlyMangaResource(mangaResource: FilteredMangaYearlyResource)


    @Delete
    suspend fun deleteFilteredYearlyMangaResource(mangaResource: FilteredMangaYearlyResource)

    companion object {
        const val id = 0
    }
}