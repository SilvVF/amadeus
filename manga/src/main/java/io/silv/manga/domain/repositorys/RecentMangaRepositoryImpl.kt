package io.silv.manga.domain.repositorys

import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.domain.MangaToRecentMangaResourceMapper
import io.silv.manga.local.dao.RecentMangaResourceDao
import io.silv.manga.local.entity.RecentMangaResource
import io.silv.manga.local.entity.syncerForEntity
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.manga.Manga
import io.silv.manga.network.mangadex.requests.MangaRequest
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext

internal class RecentMangaRepositoryImpl(
    private val mangaDexApi: MangaDexApi,
    private val mangaResourceDao: RecentMangaResourceDao,
    dispatchers: AmadeusDispatchers
): RecentMangaRepository {

    private val mapper = MangaToRecentMangaResourceMapper
    private val scope = CoroutineScope(dispatchers.io) + CoroutineName("RecentMangaRepositoryImpl")

    private val MANGA_PAGE_LIMIT = 50
    private var currentOffset: Int = 0


    private val syncer = syncerForEntity<RecentMangaResource, Manga, String>(
        networkToKey = { n -> n.id },
        mapper = { manga, resource -> mapper.map(manga to resource) },
        upsert = { mangaResourceDao.upsertManga(it) }
    )

    override fun getMangaResource(id: String): Flow<RecentMangaResource?> {
        return mangaResourceDao.getResourceAsFlowById(id)
    }

    override fun getMangaResources(): Flow<List<RecentMangaResource>> {
        return mangaResourceDao.getMangaResources()
    }

    override suspend fun loadNextPage(): Boolean = withContext(scope.coroutineContext) {
        runCatching {
            val result = syncer.sync(
                current = mangaResourceDao.getAll(),
                networkResponse = mangaDexApi.getMangaList(
                    MangaRequest(
                        offset = currentOffset,
                        limit = MANGA_PAGE_LIMIT,
                        includes = listOf("cover_art"),
                        status = listOf("ongoing"),
                        availableTranslatedLanguage = listOf("en")
                    )
                )
                    .getOrThrow()
                    .data
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