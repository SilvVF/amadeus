package io.silv.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import io.silv.database.entity.list.SeasonalListEntity
import io.silv.database.entity.manga.SourceMangaResource
import io.silv.database.entity.manga.remotekeys.SeasonalRemoteKey
import kotlinx.coroutines.flow.Flow

@Dao
interface SeasonalListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSeasonalList(seasonalListEntity: SeasonalListEntity)

    @Query("SELECT * FROM SeasonalListEntity")
    fun observeSeasonalLists(): Flow<List<SeasonalListEntity>>

    @Query("DELETE FROM SeasonalListEntity")
    suspend fun clear()

    @Query("DELETE FROM SeasonalListEntity WHERE id not in (:ids)")
    suspend fun clearNonMathcingIds(ids: List<String>)

    @Transaction
    @Query("SELECT * FROM SeasonalListEntity")
    fun observeSeasonListWithManga(): Flow<List<SeasonalListAndKeyAndManga>>
}

class SeasonalListAndKeyAndManga(
    @Embedded
    val list: SeasonalListEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "season_id",
        entity = SeasonalRemoteKey::class,
    )
    val manga: List<SeasonalKeyWithSourceManga> = emptyList(),
)

data class SeasonalKeyWithSourceManga(
    @Embedded
    val key: SeasonalRemoteKey,
    @Relation(
        parentColumn = "manga_id",
        entityColumn = "id",
    )
    val manga: SourceMangaResource,
)
