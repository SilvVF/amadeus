package io.silv.manga.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.silv.manga.local.entity.PopularMangaResource
import kotlinx.coroutines.flow.Flow

@Dao
internal interface PopularMangaResourceDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertManga(mangaResource: PopularMangaResource)


    @Query("""
          SELECT * FROM PopularMangaResource
    """)
    fun getPopularMangaResources(): Flow<List<PopularMangaResource>>

    @Query("SELECT * FROM PopularMangaResource WHERE id = :mangaId")
    fun getPopularMangaResourceById(mangaId: String): Flow<PopularMangaResource?>

    @Update
    suspend fun updatePopularMangaResource(mangaResource: PopularMangaResource)



    @Delete
    suspend fun deletePopularMangaResource(mangaResource: PopularMangaResource)

    companion object {
        const val id: Int = 5
    }
}