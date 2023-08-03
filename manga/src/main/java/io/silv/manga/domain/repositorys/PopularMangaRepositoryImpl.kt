package io.silv.manga.domain.repositorys

import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.domain.MangaToPopularMangaResourceMapper
import io.silv.manga.domain.checkProtected
import io.silv.manga.domain.repositorys.base.BasePaginatedRepository
import io.silv.manga.domain.timeStringMinus
import io.silv.manga.local.dao.PopularMangaResourceDao
import io.silv.manga.local.entity.PopularMangaResource
import io.silv.manga.local.entity.syncerForEntity
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.manga.Manga
import io.silv.manga.network.mangadex.requests.MangaRequest
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.plus
import java.time.Duration

internal class PopularMangaRepositoryImpl(
    private val mangaDexApi: MangaDexApi,
    private val mangaResourceDao: PopularMangaResourceDao,
    dispatchers: AmadeusDispatchers,
): PopularMangaRepository, BasePaginatedRepository<PopularMangaResource, Any?>(
    initialQuery = null
) {

    override val scope: CoroutineScope =
        CoroutineScope(dispatchers.io) + CoroutineName("PopularMangaRepositoryImpl")

    override fun observeMangaResources(resourceQuery: Any?): Flow<List<PopularMangaResource>> {
        return mangaResourceDao.getPopularMangaResources()
    }

    private val mapper = MangaToPopularMangaResourceMapper

    private val syncer = syncerForEntity<PopularMangaResource, Manga, String>(
        networkToKey = { n -> n.id },
        mapper = { manga, resource -> mapper.map(manga to resource) },
        upsert = { mangaResourceDao.upsertManga(it) }
    )

    override fun observeMangaResourceById(id: String): Flow<PopularMangaResource?> {
        return mangaResourceDao.getPopularMangaResourceById(id)
    }

    override fun observeAllMangaResources(): Flow<List<PopularMangaResource>> {
        return mangaResourceDao.getPopularMangaResources()
    }

    override suspend fun refresh() {
        resetPagination(null)
        loadNextPage()
    }

    override suspend fun loadNextPage() = loadPage { offset, _ ->
            val result = syncer.sync(
                current = mangaResourceDao.getPopularMangaResources().first(),
                networkResponse = mangaDexApi.getMangaList(
                    MangaRequest(
                        offset = offset,
                        limit = MANGA_PAGE_LIMIT,
                        includes = listOf("cover_art"),
                        order = mapOf("followedCount" to "desc"),
                        availableTranslatedLanguage = listOf("en"),
                        hasAvailableChapters = true,
                        createdAtSince = timeStringMinus(Duration.ofDays(30))
                    )
                )
                    .getOrThrow()
                    .also {
                        updateLastPage(it.total)
                    }
                    .data
            )
            if (offset == 0) {
                for(unhandled in result.unhandled) {
                    if (!checkProtected(unhandled.id)) {
                        mangaResourceDao.deletePopularMangaResource(unhandled)
                    }
                }
            }
    }
}