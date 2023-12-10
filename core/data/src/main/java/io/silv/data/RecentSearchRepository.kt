package io.silv.data

import io.silv.database.dao.RecentSearchDao
import io.silv.database.entity.RecentSearchEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock

interface RecentSearchRepository {
    /**
     * Get the recent search queries up to the number of queries specified as [limit].
     */
    fun getRecentSearchQueries(limit: Int): Flow<List<RecentSearchEntity>>

    /**
     * Insert or replace the [searchQuery] as part of the recent searches.
     */
    suspend fun insertOrReplaceRecentSearch(searchQuery: String)

    /**
     * Clear the recent searches.
     */
    suspend fun clearRecentSearches()
}

class RecentSearchRepositoryImpl(
    private val recentSearchQueryDao: RecentSearchDao,
) : RecentSearchRepository {
    override suspend fun insertOrReplaceRecentSearch(searchQuery: String) {
        recentSearchQueryDao.insertOrReplaceRecentSearchQuery(
            RecentSearchEntity(
                query = searchQuery,
                queriedDate = Clock.System.now(),
            ),
        )
    }

    override fun getRecentSearchQueries(limit: Int): Flow<List<RecentSearchEntity>> {
        return recentSearchQueryDao.getRecentSearchQueryEntities(limit)
    }

    override suspend fun clearRecentSearches() = recentSearchQueryDao.clearRecentSearchQueries()
}

