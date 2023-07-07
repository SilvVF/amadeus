package io.silv.manga.domain.repositorys

import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.domain.alternateTitles
import io.silv.manga.domain.coverArtUrl
import io.silv.manga.domain.descriptionEnglish
import io.silv.manga.domain.titleEnglish
import io.silv.manga.local.dao.MangaResourceDao
import io.silv.manga.local.entity.MangaResource
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.manga.Manga
import io.silv.manga.network.mangadex.requests.MangaRequest
import io.silv.core.Mapper
import io.silv.manga.local.entity.syncerForEntity
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

    private val mapper = MangaToMangaResourceMapper()
    private val scope = CoroutineScope(dispatchers.io) + CoroutineName("OfflineFirstMangaRepository")

    private val MANGA_PAGE_LIMIT = 50
    private var currentOffset: Int = 0

    private val syncer = syncerForEntity<MangaResource, Manga, String>(
        networkToKey = { n -> n.id },
        mapper = { manga, resource -> mapper.map(manga to resource) },
        upsert = { mangaResourceDao.upsertManga(it) }
    )

    override fun getMangaResource(id: String): Flow<MangaResource> {
        return mangaResourceDao.getResourceAsFlowById(id)
    }

    override fun getMangaResources(
        query: MangaQuery
    ): Flow<List<MangaResource>> = mangaResourceDao.getMangaResources()

    override suspend fun loadNextPage(): Boolean = withContext(scope.coroutineContext) {
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

        // Initial sync delete previous paging data
        if (currentOffset == 0 && result.unhandled.size > 200) {
            for(unhandled in result.unhandled.subList(100, 200)) {
                mangaResourceDao.delete(unhandled)
            }
        }
        // Increase offset after successful sync
        currentOffset += MANGA_PAGE_LIMIT
        true
    }

    private class MangaToMangaResourceMapper: Mapper<Pair<Manga, MangaResource?>, MangaResource> {

        override fun map(from: Pair<Manga, MangaResource?>): MangaResource {
            val (manga, resource) = from
            return with(manga) {
                MangaResource(
                    id = id,
                    description = manga.descriptionEnglish,
                    coverArt = coverArtUrl(manga),
                    titleEnglish = manga.titleEnglish,
                    alternateTitles = manga.alternateTitles,
                    originalLanguage = attributes.originalLanguage,
                    availableTranslatedLanguages = attributes.availableTranslatedLanguages
                        .filterNotNull(),
                    status = attributes.status,
                    contentRating = attributes.contentRating,
                    lastVolume = attributes.lastVolume,
                    lastChapter = attributes.lastChapter,
                    version = attributes.version,
                    createdAt = attributes.createdAt,
                    updatedAt = attributes.updatedAt,
                )
            }
        }
    }
}