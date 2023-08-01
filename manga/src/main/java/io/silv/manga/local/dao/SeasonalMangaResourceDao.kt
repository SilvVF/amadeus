package io.silv.manga.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.silv.manga.local.entity.SeasonalMangaResource
import kotlinx.coroutines.flow.Flow

@Dao
internal interface SeasonalMangaResourceDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertManga(mangaResource: SeasonalMangaResource)


    @Query("""
          SELECT * FROM SeasonalMangaResource ORDER BY savedLocalAtEpochSeconds ASC
    """)
    fun getMangaResources(): Flow<List<SeasonalMangaResource>>

    @Query("SELECT * FROM SeasonalMangaResource WHERE id = :mangaId")
    fun getResourceAsFlowById(mangaId: String): Flow<SeasonalMangaResource?>

    @Query("SELECT * FROM SeasonalMangaResource")
    suspend fun getAll(): List<SeasonalMangaResource>

    @Update
    suspend fun update(mangaResource: SeasonalMangaResource)


    @Query("SELECT * FROM SeasonalMangaResource WHERE id = :id LIMIT 1")
    suspend fun getMangaById(id: String):  SeasonalMangaResource?


    @Delete
    suspend fun delete(mangaResource: SeasonalMangaResource)

    companion object {
        const val id: Int = 10
    }
}