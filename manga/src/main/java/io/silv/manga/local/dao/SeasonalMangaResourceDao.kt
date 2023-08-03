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
    suspend fun upsertSeasonalMangaResource(mangaResource: SeasonalMangaResource)


    @Query("""
          SELECT * FROM SeasonalMangaResource
    """)
    fun getSeasonalMangaResources(): Flow<List<SeasonalMangaResource>>

    @Query("SELECT * FROM SeasonalMangaResource WHERE id = :mangaId")
    fun getSeasonalMangaResourceById(mangaId: String): Flow<SeasonalMangaResource?>

    @Update
    suspend fun updateSeasonalMangaResource(mangaResource: SeasonalMangaResource)

    @Delete
    suspend fun deleteSeasonalMangaResource(mangaResource: SeasonalMangaResource)

    companion object {
        const val id: Int = 10
    }
}