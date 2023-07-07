package io.silv.manga.domain.repositorys

import android.util.Log
import io.silv.core.AmadeusDispatchers
import io.silv.core.Mapper
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.domain.alternateTitles
import io.silv.manga.domain.coverArtUrl
import io.silv.manga.domain.descriptionEnglish
import io.silv.manga.domain.titleEnglish
import io.silv.manga.local.dao.MangaResourceDao
import io.silv.manga.local.dao.SavedMangaDao
import io.silv.manga.local.entity.ProgressState
import io.silv.manga.local.entity.SavedMangaEntity
import io.silv.manga.local.entity.syncerForEntity
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.manga.Manga
import io.silv.manga.sync.Synchronizer
import io.silv.manga.sync.syncWithSyncer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

internal class SavedMangaRepositoryImpl(
    private val savedMangaDao: SavedMangaDao,
    private val mangaDao: MangaResourceDao,
    private val mangaDexApi: MangaDexApi,
    private val dispatchers: AmadeusDispatchers,
): SavedMangaRepository {

    private val TAG = "SavedMangaRepositoryImpl"
    private fun log(msg: String) = Log.d(TAG, msg)

    private val mapper = MangaEntityMapper()

    private val syncer = syncerForEntity<SavedMangaEntity, Manga, String>(
        networkToKey = { manga -> manga.id },
        mapper = { network, saved -> mapper.map(network to saved) },
        upsert = { savedMangaDao.upsertManga(it) }
    )

    override suspend fun bookmarkManga(id: String) {
        CoroutineScope(dispatchers.io).launch {
            savedMangaDao.getMangaWithChapters(id)?.let { (manga, chapters) ->
                if (!manga.bookmarked) {
                    savedMangaDao.updateSavedManga(
                        manga.copy(bookmarked = true)
                    )
                } else {
                    // Check if any images are downloaded or the progress state needs to be tracked
                    if (
                        chapters.all { it.chapterImages.isNullOrEmpty() } &&
                        manga.progressState == ProgressState.NotStarted
                    ) {
                        // delete if above is true
                        savedMangaDao.delete(manga)
                    } else {
                        // need the saved manga to track progress and save the images
                        savedMangaDao.updateSavedManga(
                            manga.copy(bookmarked = false)
                        )
                    }
                }
            } ?: mangaDao.getMangaById(id)?.let { resource ->
                log("No Saved found using resource $id")
                savedMangaDao.upsertManga(
                    SavedMangaEntity(resource).copy(
                        bookmarked = true
                    )
                )
                log("Inserted Saved manga using resource $id and set bookmarked true")
            }
        }
    }

    override fun getSavedMangas(): Flow<List<SavedMangaEntity>> {
        return savedMangaDao.getAllAsFlow()
    }

    override fun getSavedManga(id: String): Flow<SavedMangaEntity?> {
        return savedMangaDao.getMangaByIdAsFlow(id)
    }

    override suspend fun syncWith(synchronizer: Synchronizer): Boolean {
        return synchronizer.syncWithSyncer(
            syncer = syncer,
            getCurrent = { savedMangaDao.getAll() },
            getNetwork = {
                savedMangaDao.getAll().map {
                    mangaDexApi.getMangaById(it.id)
                        .getOrThrow()
                        .data
                }
            },
            onComplete = { result ->
                log(
                    "Finished Sync of saved manga\n" +
                          "Unhandled: ${result.unhandled.map { it.id }}\n"  +
                          "Added: ${result.added.map { it.id }}\n"  +
                          "Updated: ${result.updated.map { it.id }}"
                )
            }
        )
    }

    private class MangaEntityMapper: Mapper<Pair<Manga, SavedMangaEntity?>, SavedMangaEntity> {

        override fun map(from: Pair<Manga, SavedMangaEntity?>): SavedMangaEntity {
            val (network, saved) = from
            return SavedMangaEntity(
                id = network.id,
                progressState = saved?.progressState ?: ProgressState.NotStarted,
                coverArt = coverArtUrl(network),
                description = network.descriptionEnglish,
                titleEnglish = network.titleEnglish,
                alternateTitles = network.alternateTitles,
                originalLanguage = network.attributes.originalLanguage,
                availableTranslatedLanguages = network.attributes.availableTranslatedLanguages
                    .filterNotNull(),
                status = network.attributes.status,
                contentRating = network.attributes.contentRating,
                lastChapter = network.attributes.lastChapter,
                lastVolume = network.attributes.lastVolume,
                version = network.attributes.version,
                bookmarked = saved?.bookmarked ?: false,
                volumeToCoverArt = saved?.volumeToCoverArt ?: emptyMap(),
                createdAt = network.attributes.createdAt,
                updatedAt = network.attributes.updatedAt,
            )
        }
    }
}