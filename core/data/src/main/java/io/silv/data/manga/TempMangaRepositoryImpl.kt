package io.silv.data.manga

import io.silv.common.coroutine.suspendRunCatching
import io.silv.data.mappers.toTempMangaResource
import io.silv.database.entity.manga.resource.TempMangaResource
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.network.requests.MangaRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class TempMangaRepositoryImpl(
    private val dao: io.silv.database.dao.TempMangaResourceDao,
    private val mangaDexApi: io.silv.network.MangaDexApi,
    private val dispatchers: io.silv.common.AmadeusDispatchers
): TempMangaRepository {

    init {
        CoroutineScope(dispatchers.io).launch { dao.clear() }
    }

    override suspend fun update(
        id: String,
        update: (TempMangaResource) -> TempMangaResource
    ) {
        withContext(dispatchers.io) {
            dao.observeTempMangaResourceById(id)
                .firstOrNull()
                ?.let {
                    dao.updateTempMangaResource(update(it))
                }
        }
    }

    override suspend fun createTempResource(id: String): Boolean = suspendRunCatching {
        withContext(dispatchers.io) {
            val manga = mangaDexApi.getMangaList(
                MangaRequest(
                    ids = listOf(id),
                    includes = listOf("cover_art", "author", "artist")
                )
            )
                .getOrThrow()
                .data
                .first()

            dao.upsertManga(
                manga.toTempMangaResource()
            )
        }
    }
        .isSuccess
}