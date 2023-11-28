package io.silv.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.silv.database.entity.manga.resource.SeasonalMangaResource
import kotlinx.coroutines.flow.Flow

@Dao
interface SeasonalMangaResourceDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSeasonalMangaResource(mangaResource: SeasonalMangaResource)


    @Query("""
          SELECT * FROM SeasonalMangaResource
    """)
    fun getSeasonalMangaResources(): Flow<List<SeasonalMangaResource>>

    @Query("SELECT * FROM SeasonalMangaResource WHERE id = :mangaId")
    fun observeSeasonalMangaResourceById(mangaId: String): Flow<SeasonalMangaResource?>

    @Update
    suspend fun updateSeasonalMangaResource(mangaResource: SeasonalMangaResource)

    @Delete
    suspend fun deleteSeasonalMangaResource(mangaResource: SeasonalMangaResource)

    @Query("DELETE FROM SeasonalMangaResource")
    suspend fun clear()

    companion object {
        const val id: Int = 10
    }
}