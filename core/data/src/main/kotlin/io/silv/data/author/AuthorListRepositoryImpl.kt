package io.silv.data.author

import com.skydoves.sandwich.getOrThrow
import io.silv.common.AmadeusDispatchers
import io.silv.common.model.QueryResult
import io.silv.domain.AuthorListRepository
import io.silv.model.DomainAuthor
import io.silv.network.MangaDexApi
import io.silv.network.requests.AuthorListRequest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

internal class AuthorListRepositoryImpl(
    private val mangaDexApi: MangaDexApi,
    private val dispatchers: AmadeusDispatchers
): AuthorListRepository {

    override fun getAuthorList(
        query: String?
    ) = flow<QueryResult<List<DomainAuthor>>> {

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
                emit(
                    QueryResult.Done(authorList.map { DomainAuthor(it.attributes.name, it.id) })
                )
            }
            .onFailure {
                emit(QueryResult.Done(emptyList()))
            }
    }
        .flowOn(dispatchers.io)
}