package io.silv.manga.repositorys.author

import io.silv.manga.network.mangadex.models.author.AuthorListResponse
import kotlinx.coroutines.flow.Flow

interface AuthorListRepository {

    fun getAuthorList(query: String?): Flow<QueryResult<List<AuthorListResponse.Author>>>
}

sealed interface QueryResult<out T> {
    object Loading: QueryResult<Nothing>
    data class Done<T>(val result: T): QueryResult<T>
}