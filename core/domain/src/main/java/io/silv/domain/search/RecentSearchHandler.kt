package io.silv.domain.search

import android.util.Log
import io.silv.common.AmadeusDispatchers
import io.silv.data.RecentSearchRepository
import io.silv.database.entity.RecentSearchEntity
import io.silv.model.RecentSearch
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RecentSearchHandler(
    private val dispatchers: AmadeusDispatchers,
    private val recentSearchRepository: RecentSearchRepository,
) {
    val recentSearchList =
        recentSearchRepository.getRecentSearchQueries(limit = 10)
            .map { list ->
                Log.d("search-history", "new list $list")
                list.map { it.toExternal() }.toImmutableList()
            }

    suspend fun onSearchTriggered(query: String) =
        withContext(dispatchers.io) {
            recentSearchRepository.insertOrReplaceRecentSearch(searchQuery = query)
        }

    suspend fun clearRecentSearches() =
        withContext(dispatchers.io) {
            Log.d("search-history", "recentSearchRepository.clearRecentSearches")
            recentSearchRepository.clearRecentSearches()
        }

    private fun RecentSearchEntity.toExternal(): RecentSearch {
        return RecentSearch(
            query = query,
            date = queriedDate,
        )
    }
}
