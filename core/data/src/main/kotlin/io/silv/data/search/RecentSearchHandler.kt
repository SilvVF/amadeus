package io.silv.data.search

import io.silv.common.AmadeusDispatchers
import io.silv.common.log.logcat
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
            logcat { "recentSearchRepository.clearRecentSearches" }
            recentSearchRepository.clearRecentSearches()
        }
}