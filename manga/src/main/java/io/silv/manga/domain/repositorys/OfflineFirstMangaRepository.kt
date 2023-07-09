package io.silv.manga.domain.repositorys

import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.domain.MangaToMangaResourceMapper
import io.silv.manga.local.dao.MangaResourceDao
import io.silv.manga.local.entity.MangaResource
import io.silv.manga.local.entity.syncerForEntity
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.manga.Manga
import io.silv.manga.network.mangadex.requests.MangaRequest
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext

internal class OfflineFirstMangaRepository(
    private val mangaDexApi: MangaDexApi,
    private val mangaResourceDao: MangaResourceDao,
    private val dispatchers: AmadeusDispatchers
): MangaRepository {

    private val mapper = MangaToMangaResourceMapper
    private val scope = CoroutineScope(dispatchers.io) + CoroutineName("OfflineFirstMangaRepository")

    private val MANGA_PAGE_LIMIT = 50
    private var currentOffset: Int = 0

    private val syncer = syncerForEntity<MangaResource, Manga, String>(
        networkToKey = { n -> n.id },
        mapper = { manga, resource -> mapper.map(manga to resource) },
        upsert = { mangaResourceDao.upsertManga(it) }
    )

    override fun getMangaResource(id: String): Flow<MangaResource?> {
        return mangaResourceDao.getResourceAsFlowById(id)
    }

    override fun getMangaResources(): Flow<List<MangaResource>> = mangaResourceDao.getMangaResources()

    override suspend fun loadNextPage(): Boolean = withContext(scope.coroutineContext) {
        runCatching {
            val result = syncer.sync(
                current = mangaResourceDao.getAll(),
                networkResponse = mangaDexApi.getMangaList(
                    MangaRequest(
                        offset = currentOffset,
                        limit = MANGA_PAGE_LIMIT,
                        includes = listOf("cover_art")
                    )
                )
                    .getOrThrow()
                    .data,
            )

            // Initial load delete previous resources
            // if at this point network response was successful
            if (currentOffset == 0) {
                for(unhandled in result.unhandled) {
                    mangaResourceDao.delete(unhandled)
                }
            }
            // Increase offset after successful sync
            currentOffset += MANGA_PAGE_LIMIT
        }
            .isSuccess
    }
}