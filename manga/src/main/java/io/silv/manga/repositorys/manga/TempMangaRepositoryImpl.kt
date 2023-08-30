package io.silv.manga.repositorys.manga

import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.local.dao.TempMangaResourceDao
import io.silv.manga.local.entity.manga_resource.TempMangaResource
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.requests.MangaRequest
import io.silv.manga.repositorys.suspendRunCatching
import io.silv.manga.repositorys.toTempMangaResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class TempMangaRepositoryImpl(
    private val dao: TempMangaResourceDao,
    private val mangaDexApi: MangaDexApi,
    private val dispatchers: AmadeusDispatchers
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