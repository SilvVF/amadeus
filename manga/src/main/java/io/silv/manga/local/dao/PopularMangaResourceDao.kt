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
          SELECT * FROM PopularMangaResource ORDER BY savedLocalAtEpochSeconds ASC
    """)
    fun getMangaResources(): Flow<List<PopularMangaResource>>

    @Query("SELECT * FROM PopularMangaResource WHERE id = :mangaId")
    fun getResourceAsFlowById(mangaId: String): Flow<PopularMangaResource?>

    @Query("SELECT * FROM PopularMangaResource")
    suspend fun getAll(): List<PopularMangaResource>

    @Update
    suspend fun update(mangaResource: PopularMangaResource)


    @Query("SELECT * FROM PopularMangaResource WHERE id = :id LIMIT 1")
    suspend fun getMangaById(id: String):  PopularMangaResource?


    @Delete
    suspend fun delete(mangaResource: PopularMangaResource)

    companion object {
        const val id = 3
    }
}