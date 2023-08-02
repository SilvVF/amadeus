package io.silv.manga.domain.repositorys.people

import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.domain.models.DomainAuthor
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.requests.AuthorListRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

internal class AuthorListRepositoryImpl(
    private val mangaDexApi: MangaDexApi,
    private val dispatchers: AmadeusDispatchers
): AuthorListRepository {

    override fun getAuthorList(
        query: String?
    ): Flow<QueryResult<List<DomainAuthor>>> = flow<QueryResult<List<DomainAuthor>>> {
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
            .onSuccess { list ->
                emit(
                    QueryResult.Done(
                        list.map {
                            DomainAuthor(name = it.attributes.name, id = it.id)
                        }
                    )
                )
            }
            .onFailure {
                it.printStackTrace()
                emit(QueryResult.Done(emptyList()))
            }
    }
        .flowOn(dispatchers.io)
}