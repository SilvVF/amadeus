package io.silv.manga.domain.repositorys

import android.util.Log
import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.domain.MangaToQuickSearchMangaResourceMapper
import io.silv.manga.domain.checkProtected
import io.silv.manga.domain.repositorys.base.BasePaginatedRepository
import io.silv.manga.local.dao.QuickSearchMangaResourceDao
import io.silv.manga.local.entity.QuickSearchMangaResource
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

internal class QuickSearchMangaRepositoryTest(
    private val mangaDexTestApi: MangaDexTestApi
) : QuickSearchMangaRepository, BasePaginatedRepository<QuickSearchMangaResource, String>(
    initialQuery = "",
    pageSize = 50
) {
    override val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    override fun observeMangaResourceById(id: String): Flow<QuickSearchMangaResource?> {
        return emptyFlow<List<QuickSearchMangaResource>>().onStart {
            emit(
            mangaDexTestApi.getMangaList().data.map { MangaToQuickSearchMangaResourceMapper.map(it to null) }
            )
        }.map { list ->
            list.firstOrNull { it.id == id }
        }
    }

    override fun observeAllMangaResources(): Flow<List<QuickSearchMangaResource>> {
        return emptyFlow<List<QuickSearchMangaResource>>().onStart {
            emit(
            mangaDexTestApi.getMangaList().data.map { MangaToQuickSearchMangaResourceMapper.map(it to null) }
            )
        }
    }

    override suspend fun loadNextPage() {

    }

    override fun observeMangaResources(resourceQuery: String): Flow<List<QuickSearchMangaResource>> {
        return emptyFlow<List<QuickSearchMangaResource>>().onStart {
            emit(
            mangaDexTestApi.getMangaList().data.map { MangaToQuickSearchMangaResourceMapper.map(it to null) }
            )
        }
    }
}

internal class QuickSearchMangaRepositoryImpl(
    private val mangaDexApi: MangaDexApi,
    private val mangaResourceDao: QuickSearchMangaResourceDao,
    dispatchers: AmadeusDispatchers,
): QuickSearchMangaRepository, BasePaginatedRepository<QuickSearchMangaResource, String>(initialQuery = "") {

    override val scope: CoroutineScope =
        CoroutineScope(dispatchers.io) + CoroutineName("QuickSearchMangaRepositoryImpl")

    private val mapper = MangaToQuickSearchMangaResourceMapper

    private val syncer = syncerForEntity<QuickSearchMangaResource, Manga, String>(
        networkToKey = { n -> n.id },
        mapper = { manga, resource -> mapper.map(manga to resource) },
        upsert = { mangaResourceDao.upsertSearchMangaResource(it) }
    )

    init {
        scope.launch {
            refresh()
        }
    }

    override fun observeMangaResourceById(id: String): Flow<QuickSearchMangaResource?> {
        return mangaResourceDao.observeQuickSearchMangaResourceById(id)
    }

    override fun observeMangaResources(resourceQuery: String): Flow<List<QuickSearchMangaResource>>  {
        return mangaResourceDao.observeAllSearchMangaResources().onStart {
            if (resourceQuery != latestQuery()) {
                emit(emptyList())
                refresh(resourceQuery)
            }
        }
    }

    override fun observeAllMangaResources(): Flow<List<QuickSearchMangaResource>> {
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
                    title = query,
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