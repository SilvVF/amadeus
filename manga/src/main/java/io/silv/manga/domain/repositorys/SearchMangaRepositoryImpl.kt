package io.silv.manga.domain.repositorys

import android.util.Log
import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.domain.MangaToSearchMangaResourceMapper
import io.silv.manga.domain.checkProtected
import io.silv.manga.domain.repositorys.base.BasePaginatedRepository
import io.silv.manga.local.dao.SearchMangaResourceDao
import io.silv.manga.local.entity.SearchMangaResource
import io.silv.manga.local.entity.syncerForEntity
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.MangaDexTestApi
import io.silv.manga.network.mangadex.models.manga.Manga
import io.silv.manga.network.mangadex.requests.MangaRequest
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

internal class SearchMangaRepositoryTest(
    private val mangaDexTestApi: MangaDexTestApi
): SearchMangaRepository, BasePaginatedRepository<SearchMangaResource, SearchMangaResourceQuery>(
    initialQuery = SearchMangaResourceQuery(),
    pageSize = 50
)  {
    override val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    override fun observeMangaResourceById(id: String): Flow<SearchMangaResource?> {
        return emptyFlow<List<SearchMangaResource>>().onStart {
            emit(
            mangaDexTestApi.getMangaList().data.map { MangaToSearchMangaResourceMapper.map(it to null) }
            )
        }.map { list ->
            list.firstOrNull { it.id == id }
        }
    }

    override fun observeAllMangaResources(): Flow<List<SearchMangaResource>> {
        return emptyFlow<List<SearchMangaResource>>().onStart {
            emit(
            mangaDexTestApi.getMangaList().data.map { MangaToSearchMangaResourceMapper.map(it to null) }
            )
        }
    }

    override suspend fun loadNextPage() {

    }

    override fun observeMangaResources(resourceQuery: SearchMangaResourceQuery): Flow<List<SearchMangaResource>> {
        return emptyFlow<List<SearchMangaResource>>().onStart {
            emit(
            mangaDexTestApi.getMangaList().data.map { MangaToSearchMangaResourceMapper.map(it to null) }
            )
        }
    }
}

internal class SearchMangaRepositoryImpl(
    private val mangaDexApi: MangaDexApi,
    private val mangaResourceDao: SearchMangaResourceDao,
    dispatchers: AmadeusDispatchers,
): SearchMangaRepository, BasePaginatedRepository<SearchMangaResource, SearchMangaResourceQuery>(
    initialQuery = SearchMangaResourceQuery(),
    pageSize = 50
) {

    override val scope: CoroutineScope =
        CoroutineScope(dispatchers.io) + CoroutineName("SearchMangaRepositoryImpl")

    private val mapper = MangaToSearchMangaResourceMapper

    private val syncer = syncerForEntity<SearchMangaResource, Manga, String>(
        networkToKey = { n -> n.id },
        mapper = { manga, resource -> mapper.map(manga to resource) },
        upsert = { mangaResourceDao.upsertSearchMangaResource(it) }
    )

    init {
        scope.launch {
            refresh(SearchMangaResourceQuery())
        }
    }

    override fun observeMangaResourceById(id: String): Flow<SearchMangaResource?> {
        return mangaResourceDao.observeSearchMangaResourceById(id)
    }

    override fun observeMangaResources(resourceQuery: SearchMangaResourceQuery): Flow<List<SearchMangaResource>>  {
        return mangaResourceDao.observeAllSearchMangaResources().onStart {
            if (resourceQuery != latestQuery()) {
                resetPagination(resourceQuery)
                emit(emptyList())
                loadNextPage()
            }
        }
    }

    override fun observeAllMangaResources(): Flow<List<SearchMangaResource>> {
        return mangaResourceDao.observeAllSearchMangaResources()
    }


    override suspend fun loadNextPage() = loadPage { offset, query ->
        val result = syncer.sync(
            current = mangaResourceDao.observeAllSearchMangaResources().first(),
            networkResponse = mangaDexApi.getMangaList(
                MangaRequest(
                    offset = offset,
                    limit = pageSize,
                    includes = listOf("cover_art","author", "artist"),
                    includedTags = query.includedTags,
                    excludedTags = query.excludedTags,
                    title = query.title,
                    includedTagsMode = query.includedTagsMode,
                    excludedTagsMode = query.excludedTagsMode,
                    status = query.publicationStatus?.map { it.name },
                    availableTranslatedLanguage = query.translatedLanguages,
                    contentRating = query.contentRating?.map { it.name },
                    authors = query.authorIds,
                    artists = query.artistIds,
                    originalLanguage = query.originalLanguages,
                    publicationDemographic = query.demographics?.map { it.name }
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
                if (!checkProtected(unhandled.id)) {
                    mangaResourceDao.delete(unhandled)
                }
            }
        }
    }
}
