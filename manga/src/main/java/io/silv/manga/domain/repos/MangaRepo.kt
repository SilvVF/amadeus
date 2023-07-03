package io.silv.manga.domain.repos

import MangaEntityToDomainMangaMapper
import MangaToMangaEntityMapper
import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.ApiResponse
import io.silv.ktor_response_mapper.suspendMapSuccess
import io.silv.manga.domain.models.DomainManga
import io.silv.manga.local.dao.MangaDao
import io.silv.manga.local.entity.MangaEntity
import io.silv.manga.local.entity.ProgressState
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.manga.Manga
import io.silv.manga.network.mangadex.requests.MangaRequest
import io.silv.manga.sync.Mapper
import io.silv.manga.sync.syncerForEntity
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus


class MangaRepo(
    private val mangaDexApi: MangaDexApi,
    private val mangaDao: MangaDao,
    private val dispatchers: AmadeusDispatchers,
) {

    private val mangaEntityToDomainMapper: Mapper<MangaEntity, DomainManga> = MangaEntityToDomainMangaMapper
    private val networkToLocalMapper: Mapper<Pair<Manga, MangaEntity?>, MangaEntity> = MangaToMangaEntityMapper

    private val scope = CoroutineScope(dispatchers.io) + CoroutineName("MANGA_REPO_IO_SCOPE")

    private val mangaSyncer = syncerForEntity<MangaEntity, Manga, String>(
        entityDao = mangaDao,
        networkToKey = { manga -> manga.id },
        mapper = { networkValue, localValue -> networkToLocalMapper.map(networkValue to localValue) }
    )

    suspend fun getBookmarkedManga(): List<DomainManga> {
        return mangaDao.getAll()
            .filter { it.bookmarked }
            .map { mangaEntityToDomainMapper.map(it) }
    }

    suspend fun getStartedManga(): List<DomainManga> {
        return mangaDao.getAll()
            .filter { it.progressState != ProgressState.NotStarted }
            .map { mangaEntityToDomainMapper.map(it) }
    }

    /**
     * Returns full manga list
     */
    suspend fun getMangaList(
        offset: Int = 0,
        amount: Int = 50
    ): ApiResponse<List<DomainManga>> {
        return mangaDexApi.getMangaList(
            MangaRequest(
                limit = amount,
                offset = offset,
                includes = listOf("cover_art"),
            )
        ).suspendMapSuccess {
            // if first fetch and offset is 0 delete any previous entries that were for
            // the manga list only and not saved by user
            if (offset == 0) {
                mangaDao.getAll().forEach { entity ->
                    if (entity.forList) { mangaDao.delete(entity) }
                }
            }
            // Fetch manga list from network and insert to local db
            val result = mangaSyncer.sync(mangaDao.getAll(), data)

            //only return values that are unsaved by user
            (result.added).map {
                mangaEntityToDomainMapper.map(it)
            }
        }
    }

}


