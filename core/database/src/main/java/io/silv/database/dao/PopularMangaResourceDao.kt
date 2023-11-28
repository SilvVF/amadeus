package io.silv.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.silv.database.entity.manga.resource.PopularMangaResource
import kotlinx.coroutines.flow.Flow

@Dao
interface PopularMangaResourceDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertManga(mangaResource: PopularMangaResource)


    @Query("""
          SELECT * FROM PopularMangaResource
    """)
    fun getPopularMangaResources(): Flow<List<PopularMangaResource>>

    @Query("SELECT * FROM PopularMangaResource WHERE id = :mangaId")
    fun observePopularMangaResourceById(mangaId: String): Flow<PopularMangaResource?>

    @Update
    suspend fun updatePopularMangaResource(mangaResource: PopularMangaResource)

    @Query("DELETE FROM PopularMangaResource")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(mangaResources: List<PopularMangaResource>)

    @Delete
    suspend fun deletePopularMangaResource(mangaResource: PopularMangaResource)

    @Query("SELECT * FROM PopularMangaResource")
    fun pagingSource(): PagingSource<Int, PopularMangaResource>

    companion object {
        const val id: Int = 5
    }
}