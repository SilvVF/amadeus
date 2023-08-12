package io.silv.manga.domain.usecase

import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.ApiResponse
import io.silv.ktor_response_mapper.message
import io.silv.manga.domain.repositorys.base.Resource
import io.silv.manga.local.dao.ChapterDao
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.manga.MangaAggregateResponse
import io.silv.manga.network.mangadex.requests.MangaAggregateRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn


fun interface GetMangaAggregate: (String) -> Flow<Resource<MangaAggregateResponse>> {
    companion object {
        internal fun defaultImpl(
            mangaDexApi: MangaDexApi,
            dispatchers: AmadeusDispatchers,
            chapterDao: ChapterDao
        ) = getMangaAggregateImpl(mangaDexApi,chapterDao, dispatchers)
    }
}

private val sessionAggregateResponses = mutableMapOf<String, MangaAggregateResponse>()
private val keyOrder = ArrayDeque<String>()

private fun getMangaAggregateImpl(
    mangaDexApi: MangaDexApi,
    chapterDao: ChapterDao,
    dispatchers: AmadeusDispatchers
) = GetMangaAggregate { id ->
    flow {
        emit(Resource.Loading)
        sessionAggregateResponses[id]?.let {
            emit(Resource.Success(it))
            return@flow
        }
        val result = mangaDexApi.getMangaAggregate(
            mangaId = id,
            MangaAggregateRequest(
                translatedLanguage = listOf("en"),
                groups =  chapterDao.getChaptersByMangaId(id).mapNotNull { it.scanlationGroupId }.ifEmpty { null }
            )
        )
        when(result) {
            is ApiResponse.Failure -> {
                emit(Resource.Failure(result.message(), null))
            }
            is ApiResponse.Success -> {
                if (sessionAggregateResponses.size > 10) {
                    repeat(5) {
                        sessionAggregateResponses.remove(keyOrder.removeFirst())
                    }
                }
                sessionAggregateResponses[id] = result.data
                keyOrder.addLast(id)
                emit(Resource.Success(result.data))
            }
        }
    }
        .flowOn(dispatchers.io)
}
