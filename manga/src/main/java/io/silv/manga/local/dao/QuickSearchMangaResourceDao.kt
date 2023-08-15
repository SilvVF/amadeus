package io.silv.manga.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.silv.manga.local.entity.QuickSearchMangaResource
import kotlinx.coroutines.flow.Flow

@Dao
internal interface QuickSearchMangaResourceDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSearchMangaResource(mangaResource: QuickSearchMangaResource)


    @Query("SELECT * FROM QuickSearchMangaResource")
    fun observeAllSearchMangaResources(): Flow<List<QuickSearchMangaResource>>

    @Query("SELECT * FROM QuickSearchMangaResource WHERE id = :mangaId")
    fun observeQuickSearchMangaResourceById(mangaId: String): Flow<QuickSearchMangaResource?>

    @Update
    suspend fun updateSearchMangaResource(mangaResource: QuickSearchMangaResource)

    @Delete
    suspend fun delete(mangaResource: QuickSearchMangaResource)

    @Query("DELETE FROM QuickSearchMangaResource")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(mangaResources: List<QuickSearchMangaResource>)

    @Query("SELECT * FROM QuickSearchMangaResource")
    fun pagingSource(): PagingSource<Int, QuickSearchMangaResource>

    companion object {
        const val id: Int = 2342
    }
}
