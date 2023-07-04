package io.silv.manga.domain.repositorys

import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.ApiResponse
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.ktor_response_mapper.message
import io.silv.manga.local.dao.MangaResourceDao
import io.silv.manga.local.entity.MangaResource
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.LocalizedString
import io.silv.manga.network.mangadex.models.manga.Manga
import io.silv.manga.network.mangadex.requests.MangaRequest
import io.silv.manga.sync.Mapper
import io.silv.manga.sync.Synchronizer
import io.silv.manga.sync.syncWithSyncer
import io.silv.manga.sync.syncerForEntity
import kotlinx.coroutines.flow.Flow

internal class OfflineFirstMangaRepository(
    private val mangaDexApi: MangaDexApi,
    private val mangaResourceDao: MangaResourceDao,
): MangaRepository {

    private val MANGA_PAGE_LIMIT = 50
    private var currentOffset: Int = 0

    private val mapper = MangaToMangaResourceMapper()

    private val syncer = syncerForEntity<MangaResource, Manga, String>(
        networkToKey = { n -> n.id },
        mapper = { manga, resource -> mapper.map(manga to resource) },
        upsert = { mangaResourceDao.upsertManga(it) }
    )

    override fun getMagnaResources(
        query: MangaQuery
    ): Flow<List<MangaResource>> = mangaResourceDao.getMangaResources()

    override suspend fun syncWith(synchronizer: Synchronizer, params: Nothing?): Boolean {
        return synchronizer.syncWithSyncer(
            syncer = syncer,
            getCurrent = {
                mangaResourceDao.getAll()
            },
            getNetwork = {
                mangaDexApi.getMangaList(
                    MangaRequest(
                        offset = currentOffset,
                        limit = MANGA_PAGE_LIMIT,
                        includes = listOf("cover_art")
                    )
                )
                    .getOrThrow()
                    .data
            },
            onComplete = { result ->
                // Initial sync delete previous paging data
                if (currentOffset == 0 && result.unhandled.size > 200) {
                    for(unhandled in result.unhandled.subList(100, 200)) {
                        mangaResourceDao.delete(unhandled)
                    }
                }
                // Increase offset after successful sync
                currentOffset += MANGA_PAGE_LIMIT
            }
        )
    }

    private class MangaToMangaResourceMapper: Mapper<Pair<Manga, MangaResource?>, MangaResource> {

        override fun map(from: Pair<Manga, MangaResource?>): MangaResource {
            val (manga, resource) = from
            return with(manga) {
                val altTitles = buildMap {
                    manga.attributes.altTitles.forEach { langToTitle: LocalizedString ->
                        put(
                            langToTitle.keys.firstOrNull() ?: return@forEach,
                            langToTitle.values.firstOrNull() ?: return@forEach
                        )
                    }
                }

                val fileName = relationships.find { it.type == "cover_art" }?.attributes?.get("fileName")

                MangaResource(
                    id = id,
                    description = attributes.description.getOrDefault("en", "No english description"),
                    coverArt = "https://uploads.mangadex.org/covers/${id}/$fileName",
                    titleEnglish = attributes.title.getOrDefault("en", "No english title"),
                    alternateTitles = altTitles,
                    originalLanguage = attributes.originalLanguage,
                    availableTranslatedLanguages = attributes.availableTranslatedLanguages.filterNotNull(),
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