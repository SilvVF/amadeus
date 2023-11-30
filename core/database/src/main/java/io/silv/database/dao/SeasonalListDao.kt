package io.silv.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.silv.database.entity.list.SeasonalListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SeasonalListDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSeasonalList(seasonalListEntity: SeasonalListEntity)

    @Query("SELECT * FROM SeasonalListEntity")
    fun getSeasonalLists(): Flow<List<SeasonalListEntity>>


    @Query("DELETE FROM SeasonalListEntity")
    suspend fun clear()
}