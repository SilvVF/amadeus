package io.silv.manga.domain.repositorys

import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.domain.MangaToFilteredMangaResourceMapper
import io.silv.manga.domain.checkProtected
import io.silv.manga.domain.repositorys.base.BasePaginatedRepository
import io.silv.manga.domain.timeString
import io.silv.manga.local.dao.FilteredMangaResourceDao
import io.silv.manga.local.entity.FilteredMangaResource
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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.plus

internal class FilteredMangaRepositoryTest(
    private val mangaDexTestApi: MangaDexTestApi
): FilteredMangaRepository, BasePaginatedRepository<FilteredMangaResource, FilteredResourceQuery?>(
initialQuery = null
){
    override val scope: CoroutineScope
        get() = CoroutineScope(Dispatchers.IO)

    override fun observeMangaResourceById(id: String): Flow<FilteredMangaResource?> {
        return emptyFlow<FilteredMangaResource?>().onStart {
           emit(
               MangaToFilteredMangaResourceMapper.map(
                   mangaDexTestApi.getMangaList().data.first() to null
               )
           )
        }
    }

    override fun observeAllMangaResources(): Flow<List<FilteredMangaResource>> {
        return emptyFlow<List<FilteredMangaResource>>().onStart {
            emit(
                mangaDexTestApi.getMangaList().data.map {
                    MangaToFilteredMangaResourceMapper.map(
                       it to null
                    )
                }
            )
        }
    }

    override suspend fun loadNextPage() {

    }

    override fun observeMangaResources(resourceQuery: FilteredResourceQuery?): Flow<List<FilteredMangaResource>> {
        return emptyFlow<List<FilteredMangaResource>>().onStart {
            emit(
                mangaDexTestApi.getMangaList().data.map {
                    MangaToFilteredMangaResourceMapper.map(
                        it to null
                    )
                }
            )
        }
    }
}

data class FilteredResourceQuery(
    val tagId: String,
    val timePeriod: FilteredMangaRepository.TimePeriod = FilteredMangaRepository.TimePeriod.AllTime
)

internal class FilteredMangaRepositoryImpl(
    private val resourceDao: FilteredMangaResourceDao,
    private val mangaDexApi: MangaDexApi,
    dispatchers: AmadeusDispatchers,
): FilteredMangaRepository, BasePaginatedRepository<FilteredMangaResource, FilteredResourceQuery?>(
    initialQuery = null
) {

    override val scope: CoroutineScope =
        CoroutineScope(dispatchers.io) + CoroutineName("FilteredMangaRepositoryImpl")

    private val syncer = syncerForEntity<FilteredMangaResource, Manga, String>(
        networkToKey = { it.id },
        mapper = { manga, saved ->
            MangaToFilteredMangaResourceMapper.map(
                manga to saved
            )
        },
        upsert = {
            resourceDao.upsertFilteredMangaResource(it)
        }
    )

    override fun observeMangaResourceById(id: String): Flow<FilteredMangaResource?> {
        return resourceDao.observeFilteredMangaResourceById(id)
    }

    override fun observeAllMangaResources(): Flow<List<FilteredMangaResource>> {
        return resourceDao.getFilteredMangaResources()
    }

    override fun observeMangaResources(resourceQuery: FilteredResourceQuery?): Flow<List<FilteredMangaResource>> {
        return resourceDao.getFilteredMangaResources()
            .onStart {
                if (resourceQuery != latestQuery()) {
                    emit(emptyList())
                    refresh(resourceQuery)
                }
            }
    }

    override suspend fun loadNextPage() = loadPage { offset, query ->
          query?.let {
              val result = syncer.sync(
                  current = resourceDao.getFilteredMangaResources().first(),
                  networkResponse = mangaDexApi.getMangaList(
                      MangaRequest(
                          offset = offset,
                          limit = pageSize,
                          includes = listOf("cover_art","author", "artist"),
                          availableTranslatedLanguage = listOf("en"),
                          hasAvailableChapters = true,
                          order = mapOf("followedCount" to "desc"),
                          includedTags = listOf(query.tagId),
                          includedTagsMode = MangaRequest.TagsMode.AND,
                          createdAtSince = query.timePeriod.timeString()
                      )
                  )
                      .getOrThrow()
                      .also {
                          updateLastPage(it.total)
                      }
                      .data
              )
              if (offset == 0) {
                  for (unhandled in result.unhandled) {
                      if (!checkProtected(unhandled.id)) {
                          resourceDao.deleteFilteredMangaResource(unhandled)
                      }
                  }
              }
          }
    }
}