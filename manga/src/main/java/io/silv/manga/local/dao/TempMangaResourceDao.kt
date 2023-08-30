package io.silv.manga.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.silv.manga.local.entity.manga_resource.TempMangaResource
import kotlinx.coroutines.flow.Flow

@Dao
internal interface TempMangaResourceDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertManga(mangaResource: TempMangaResource)


    @Query("""
          SELECT * FROM TempMangaResource
    """)
    fun getTempMangaResources(): Flow<List<TempMangaResource>>

    @Query("SELECT * FROM TempMangaResource WHERE id = :mangaId")
    fun observeTempMangaResourceById(mangaId: String): Flow<TempMangaResource?>

    @Update
    suspend fun updateTempMangaResource(mangaResource: TempMangaResource)

    @Query("DELETE FROM TempMangaResource")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(mangaResources: List<TempMangaResource>)

    @Delete
    suspend fun deletePopularMangaResource(mangaResource: TempMangaResource)

    @Query("DELETE FROM TempMangaResource")
    suspend fun clear()

    companion object {
        const val id: Int = 128
    }
}