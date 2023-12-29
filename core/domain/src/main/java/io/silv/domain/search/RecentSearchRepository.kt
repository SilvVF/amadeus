package io.silv.domain.search

import io.silv.model.RecentSearch
import kotlinx.coroutines.flow.Flow

interface RecentSearchRepository {
    /**
     * Get the recent search queries up to the number of queries specified as [limit].
     */
    fun getRecentSearchQueries(limit: Int): Flow<List<RecentSearch>>

    /**
     * Insert or replace the [searchQuery] as part of the recent searches.
     */
    suspend fun insertOrReplaceRecentSearch(searchQuery: String)

    /**
     * Clear the recent searches.
     */
    suspend fun clearRecentSearches()
}
