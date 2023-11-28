package io.silv.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.silv.database.entity.manga.resource.FilteredMangaResource
import kotlinx.coroutines.flow.Flow

@Dao
interface FilteredMangaResourceDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFilteredMangaResource(mangaResource: FilteredMangaResource)



    @Query("SELECT * FROM FilteredMangaResource WHERE id = :mangaId")
    fun observeFilteredMangaResourceById(mangaId: String): Flow<FilteredMangaResource?>

    @Query("SELECT * FROM FilteredMangaResource")
    fun getFilteredMangaResources(): Flow<List<FilteredMangaResource>>

    @Update
    suspend fun updateFilteredMangaResource(mangaResource: FilteredMangaResource)


    @Delete
    suspend fun deleteFilteredMangaResource(mangaResource: FilteredMangaResource)

    @Query("DELETE FROM FilteredMangaResource")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(mangaResources: List<FilteredMangaResource>)

    @Query("SELECT * FROM FilteredMangaResource")
    fun pagingSource(): PagingSource<Int, FilteredMangaResource>

    companion object {
        const val id: Int = 3
    }
}
