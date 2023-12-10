package io.silv.domain

import io.silv.data.RecentSearchRepository
import io.silv.database.entity.RecentSearchEntity
import io.silv.model.RecentSearch
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map

class RecentSearchHandler(
    private val recentSearchRepository: RecentSearchRepository
) {

    val recentSearchList = recentSearchRepository.getRecentSearchQueries(limit = 10)
        .map { list ->
            list.map { it.toExternal() } .toImmutableList()
        }

    suspend fun onSearchTriggered(query: String) {
        recentSearchRepository.insertOrReplaceRecentSearch(searchQuery = query)
    }

    suspend fun clearRecentSearches() {
        recentSearchRepository.clearRecentSearches()
    }

    private fun RecentSearchEntity.toExternal(): RecentSearch {
        return RecentSearch(
            query = query,
            date = queriedDate
        )
    }
}