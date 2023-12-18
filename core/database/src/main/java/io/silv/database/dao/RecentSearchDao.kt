package io.silv.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.silv.database.entity.RecentSearchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentSearchDao {
    @Query(value = "SELECT * FROM RecentSearchEntity ORDER BY queriedDate DESC LIMIT :limit")
    fun getRecentSearchQueryEntities(limit: Int): Flow<List<RecentSearchEntity>>

    @Upsert
    suspend fun insertOrReplaceRecentSearchQuery(recentSearchQuery: RecentSearchEntity)

    @Query(value = "DELETE FROM RecentSearchEntity")
    suspend fun clearRecentSearchQueries()
}
