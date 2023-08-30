package io.silv.manga.repositorys.author

import io.silv.manga.network.mangadex.models.author.AuthorListResponse
import kotlinx.coroutines.flow.Flow


/**
 * Repository for finding authors of a manga
 * @property getAuthorList returns a list of authors that match the query string.
 * The authors are found through querying the manga dex api author endpoint.
 */
interface AuthorListRepository {

    fun getAuthorList(query: String?): Flow<QueryResult<List<AuthorListResponse.Author>>>
}

sealed interface QueryResult<out T> {
    object Loading: QueryResult<Nothing>
    data class Done<T>(val result: T): QueryResult<T>
}