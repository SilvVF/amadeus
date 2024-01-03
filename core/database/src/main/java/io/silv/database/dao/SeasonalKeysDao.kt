package io.silv.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.silv.database.entity.manga.MangaToListRelation
import kotlinx.coroutines.flow.Flow

@Dao
interface SeasonalKeysDao {
    @Delete
    suspend fun delete(key: MangaToListRelation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: MangaToListRelation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(keys: List<MangaToListRelation>)

    @Query("DELETE FROM MangaToListRelation")
    suspend fun clear()

    @Transaction
    @Query(
        """
      SELECT * FROM MangaToListRelation
      WHERE season_id = :id
    """,
    )
    fun observeBySeasonId(id: String): Flow<List<SeasonalKeyWithSourceManga>>

    @Transaction
    @Query(
        """
      SELECT * FROM MangaToListRelation
      WHERE season_id = :id
    """,
    )
    fun selectBySeasonId(id: String): List<SeasonalKeyWithSourceManga>

    @Query(
        "DELETE FROM MangaToListRelation WHERE manga_id = :mangaId AND season_id = :seasonId",
    )
    suspend fun delete(
        mangaId: String,
        seasonId: String,
    )
}
