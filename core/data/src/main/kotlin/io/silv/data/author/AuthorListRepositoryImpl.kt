package io.silv.data.author

import com.skydoves.sandwich.getOrThrow
import io.silv.common.AmadeusDispatchers
import io.silv.common.model.QueryResult
import io.silv.network.MangaDexApi
import io.silv.network.model.author.AuthorListResponse
import io.silv.network.requests.AuthorListRequest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

internal class AuthorListRepositoryImpl(
    private val mangaDexApi: MangaDexApi,
    private val dispatchers: AmadeusDispatchers
): AuthorListRepository {

    override fun getAuthorList(
        query: String?
    ) = flow<QueryResult<List<AuthorListResponse.Author>>> {

        if (query.isNullOrBlank()) {
            emit(QueryResult.Done(emptyList()))
            return@flow
        }

        emit(QueryResult.Loading)

        runCatching {
            mangaDexApi.getAuthorList(
                AuthorListRequest(
                    limit = 10,
                    offset = 0,
                    name = query
                )
            )
                .getOrThrow()
                .data
        }
            .onSuccess { authorList ->
                emit(QueryResult.Done(authorList))
            }
            .onFailure {
                emit(QueryResult.Done(emptyList()))
            }
    }
        .flowOn(dispatchers.io)
}