package io.silv.manga.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.silv.manga.local.entity.SeasonalListEntity
import io.silv.manga.local.entity.relations.SeasonListWithManga
import kotlinx.coroutines.flow.Flow

@Dao
interface SeasonalListDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSeasonalList(seasonalListEntity: SeasonalListEntity)

    @Query("SELECT * FROM SeasonalListEntity")
    fun getSeasonalLists(): Flow<List<SeasonalListEntity>>

    @Transaction
    @Query("SELECT * FROM SeasonalListEntity")
    fun observeSeasonListWithManga(): Flow<List<SeasonListWithManga>>

    @Transaction
    @Query("SELECT * FROM SeasonalListEntity")
    fun getSeasonListWithManga(): List<SeasonListWithManga>

    @Query("DELETE FROM SeasonalListEntity")
    suspend fun clear()

    companion object {
        const val id: Int = 9
    }
}