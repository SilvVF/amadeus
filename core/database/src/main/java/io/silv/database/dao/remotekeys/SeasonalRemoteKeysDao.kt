package io.silv.database.dao.remotekeys

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.silv.database.entity.manga.SourceMangaResource
import io.silv.database.entity.manga.remotekeys.SeasonalRemoteKey

@Dao
interface SeasonalRemoteKeysDao {

    @Delete
    suspend fun delete(key: SeasonalRemoteKey)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: SeasonalRemoteKey)

    @Query("DELETE FROM SeasonalRemoteKey")
    suspend fun clear()

    @Query("""
      SELECT * FROM SourceMangaResource
      WHERE id in (
            SELECT id FROM SeasonalRemoteKey WHERE season_id = :id
      )
    """)
    suspend fun selectBySeasonId(id: String): List<SourceMangaResource>

    @Query(
        "DELETE FROM SeasonalRemoteKey WHERE manga_id = :mangaId AND season_id = :seasonId"
    )
    suspend fun delete(mangaId: String, seasonId: String)
}