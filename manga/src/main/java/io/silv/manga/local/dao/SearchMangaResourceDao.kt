package io.silv.manga.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.silv.manga.local.entity.SearchMangaResource
import kotlinx.coroutines.flow.Flow


@Dao
internal interface SearchMangaResourceDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSearchMangaResource(mangaResource: SearchMangaResource)


    @Query("SELECT * FROM SearchMangaResource")
    fun observeAllSearchMangaResources(): Flow<List<SearchMangaResource>>

    @Query("SELECT * FROM SearchMangaResource WHERE id = :mangaId")
    fun observeSearchMangaResourceById(mangaId: String): Flow<SearchMangaResource?>

    @Update
    suspend fun updateSearchMangaResource(mangaResource: SearchMangaResource)

    @Delete
    suspend fun delete(mangaResource: SearchMangaResource)


    @Query("DELETE FROM SearchMangaResource")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(mangaResources: List<SearchMangaResource>)

    @Query("SELECT * FROM SearchMangaResource")
    fun pagingSource(): PagingSource<Int, SearchMangaResource>

    companion object {
        const val id: Int = 8
    }
}
