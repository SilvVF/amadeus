package io.silv.domain.search

import android.util.Log
import io.silv.common.AmadeusDispatchers
import kotlinx.coroutines.withContext

class RecentSearchHandler(
    private val dispatchers: AmadeusDispatchers,
    private val recentSearchRepository: RecentSearchRepository,
) {

    val recentSearchList = recentSearchRepository.getRecentSearchQueries(limit = 10)

    suspend fun onSearchTriggered(query: String) =
        withContext(dispatchers.io) {
            recentSearchRepository.insertOrReplaceRecentSearch(searchQuery = query)
        }

    suspend fun clearRecentSearches() =
        withContext(dispatchers.io) {
            Log.d("search-history", "recentSearchRepository.clearRecentSearches")
            recentSearchRepository.clearRecentSearches()
        }
}