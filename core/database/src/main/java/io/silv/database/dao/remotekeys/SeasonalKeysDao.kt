package io.silv.database.dao.remotekeys

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.silv.database.dao.SeasonalKeyWithSourceManga
import io.silv.database.entity.manga.remotekeys.SeasonalRemoteKey
import kotlinx.coroutines.flow.Flow

@Dao
interface SeasonalKeysDao {

    @Delete
    suspend fun delete(key: SeasonalRemoteKey)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: SeasonalRemoteKey)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(keys: List<SeasonalRemoteKey>)

    @Query("DELETE FROM SeasonalRemoteKey")
    suspend fun clear()

    @Transaction
    @Query("""
      SELECT * FROM SeasonalRemoteKey
      WHERE season_id = :id
    """)
    fun observeBySeasonId(id: String): Flow<List<SeasonalKeyWithSourceManga>>

    @Transaction
    @Query("""
      SELECT * FROM SeasonalRemoteKey
      WHERE season_id = :id
    """)
    fun selectBySeasonId(id: String): List<SeasonalKeyWithSourceManga>


    @Query(
        "DELETE FROM SeasonalRemoteKey WHERE manga_id = :mangaId AND season_id = :seasonId"
    )
    suspend fun delete(mangaId: String, seasonId: String)
}
