package io.silv.manga.domain.repositorys

import android.util.Log
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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext

internal class SearchMangaRepositoryImpl(
    private val mangaDexApi: MangaDexApi,
    private val mangaResourceDao: SearchMangaResourceDao,
    dispatchers: AmadeusDispatchers,
): SearchMangaRepository, PaginatedResourceRepo<SearchMangaResource>() {

    private val scope: CoroutineScope = CoroutineScope(dispatchers.io) + CoroutineName("SearchMangaRepositoryImpl")
    private val mapper = MangaToSearchMangaResourceMapper

    private var currentQuery: ResourceQuery? = null

    private val syncer = syncerForEntity<SearchMangaResource, Manga, String>(
        networkToKey = { n -> n.id },
        mapper = { manga, resource -> mapper.map(manga to resource) },
        upsert = { mangaResourceDao.upsertManga(it) }
    )

    override fun getMangaResource(id: String): Flow<SearchMangaResource?> {
        return mangaResourceDao.getResourceAsFlowById(id)
    }

    override fun getMangaResources(query: ResourceQuery): Flow<List<SearchMangaResource>> {
        return mangaResourceDao.getMangaResources().onStart {
            resetPagination()
            currentQuery = query
            scope.launch {
                loadNextPage()
            }
        }
    }

    override fun getMangaResources(): Flow<List<SearchMangaResource>> {
        return mangaResourceDao.getMangaResources()
    }

    override suspend fun loadNextPage() = withContext(scope.coroutineContext) {
        Log.d("Search", "load next page")
        loadPage(currentQuery) { offset, query ->
            Log.d("Search", "loadPage")
            val result = syncer.sync(
                current = mangaResourceDao.getAll(),
                networkResponse = mangaDexApi.getMangaList(
                    MangaRequest(
                        offset = offset,
                        limit = MANGA_PAGE_LIMIT,
                        includes = listOf("cover_art"),
                        includedTags = query?.includedTags,
                        excludedTags = query?.excludedTags,
                        title = query?.title,
                        includedTagsMode = query?.includedTagsMode,
                        excludedTagsMode = query?.excludedTagsMode,
                        status = query?.publicationStatus?.map { it.name },
                        availableTranslatedLanguage = listOf("en")
                    )
                )
                    .getOrThrow()
                    .also {
                        updateLastPage(it.total)
                        Log.d("Search", "last page ${it.total}")
                    }
                    .data
            )
            if (offset == 0) {
                for(unhandled in result.unhandled) {
                    mangaResourceDao.delete(unhandled)
                }
            }
        }
    }
}