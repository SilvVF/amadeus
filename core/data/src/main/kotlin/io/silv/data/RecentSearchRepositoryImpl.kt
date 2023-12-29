package io.silv.data

import io.silv.database.dao.RecentSearchDao
import io.silv.database.entity.RecentSearchEntity
import io.silv.domain.search.RecentSearchRepository
import io.silv.model.RecentSearch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

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

    override fun getRecentSearchQueries(limit: Int): Flow<List<RecentSearch>> {
        return recentSearchQueryDao.getRecentSearchQueryEntities(limit)
            .map { entities ->
                entities.map { it.toExternal() }
            }
    }

    override suspend fun clearRecentSearches() = recentSearchQueryDao.clearRecentSearchQueries()


    private fun RecentSearchEntity.toExternal(): RecentSearch {
        return RecentSearch(
            query = query,
            date = queriedDate,
        )
    }
}

