package io.silv.domain

import io.silv.common.model.QueryResult
import io.silv.model.DomainAuthor
import kotlinx.coroutines.flow.Flow


/**
 * Repository for finding authors of a manga
 * @property getAuthorList returns a list of authors that match the query string.
 * The authors are found through querying the manga dex api author endpoint.
 */
interface AuthorListRepository {

    fun getAuthorList(query: String?): Flow<QueryResult<List<DomainAuthor>>>
}