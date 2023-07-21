package io.silv.manga.domain.repositorys

import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.domain.MangaToSearchMangaResourceMapper
import io.silv.manga.local.dao.SearchMangaResourceDao
import io.silv.manga.local.entity.SearchMangaResource
import io.silv.manga.local.entity.syncerForEntity
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.manga.Manga
import io.silv.manga.network.mangadex.requests.MangaRequest
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext

internal class SearchMangaRepositoryImpl(
    private val mangaDexApi: MangaDexApi,
    private val mangaResourceDao: SearchMangaResourceDao,
    dispatchers: AmadeusDispatchers
): SearchMangaRepository {

    private val mapper = MangaToSearchMangaResourceMapper
    private val scope = CoroutineScope(dispatchers.io) + CoroutineName("SearchMangaRepositoryImpl")

    private val MANGA_PAGE_LIMIT = 50

    private val syncer = syncerForEntity<SearchMangaResource, Manga, String>(
        networkToKey = { n -> n.id },
        mapper = { manga, resource -> mapper.map(manga to resource) },
        upsert = { mangaResourceDao.upsertManga(it) }
    )

    override fun getMangaResource(id: String): Flow<SearchMangaResource?> {
        return mangaResourceDao.getResourceAsFlowById(id)
    }

    override fun getMangaResources(query: ResourceQuery): Flow<List<SearchMangaResource>> {
        scope.launch { loadData(query) }
        return mangaResourceDao.getMangaResources()
    }

    private suspend fun loadData(query: ResourceQuery): Boolean = withContext(scope.coroutineContext) {
        runCatching {
            val result = syncer.sync(
                current = mangaResourceDao.getAll(),
                networkResponse = mangaDexApi.getMangaList(
                    MangaRequest(
                        offset = 0,
                        limit = MANGA_PAGE_LIMIT,
                        includes = listOf("cover_art"),
                        includedTags = query.includedTags,
                        excludedTags = query.excludedTags,
                        title = query.title,
                        hasAvailableChapter = true,
                        availableTranslatedLanguage = listOf("en")
                    )
                )
                    .getOrThrow()
                    .data
            )

            for(unhandled in result.unhandled) {
                mangaResourceDao.delete(unhandled)
            }
        }
            .isSuccess
    }
}