package io.silv.data.manga

import android.util.Log
import io.silv.common.model.ProgressState
import io.silv.common.model.Status
import io.silv.common.time.localDateTimeNow
import io.silv.data.mappers.toSavedManga
import io.silv.data.util.GetMangaResourcesById
import io.silv.data.util.UpdateChapterList
import io.silv.data.util.createSyncer
import io.silv.data.util.syncUsing
import io.silv.data.workers.cover_art.CoverArtHandler
import io.silv.database.dao.SavedMangaDao
import io.silv.database.entity.manga.SavedMangaEntity
import io.silv.database.entity.relations.SavedMangaWithChapters
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.network.model.manga.Manga
import io.silv.network.requests.MangaRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

internal class SavedMangaRepositoryImpl(
    private val savedMangaDao: SavedMangaDao,
    private val getMangaResourceById: GetMangaResourcesById,
    private val mangaDexApi: io.silv.network.MangaDexApi,
    private val dispatchers: io.silv.common.AmadeusDispatchers,
    private val updateChapterList: UpdateChapterList,
    private val coverArtManager: CoverArtHandler,
    private val mangaUpdateRepository: MangaUpdateRepository
): SavedMangaRepository {

    private val TAG = "SavedMangaRepositoryImpl"

    private fun log(msg: String) = Log.d(TAG, msg)

    override suspend fun bookmarkManga(id: String): Unit = withContext(dispatchers.io) {
            log("bookmarking $id")
            savedMangaDao.getSavedMangaWithChaptersById(id).first()?.let { (manga, chapters) ->
                log("saved found $id")
                if (!manga.bookmarked) {
                    savedMangaDao.updateSavedManga(
                        manga.copy(bookmarked = true)
                    )
                } else {
                    // Check if any images are downloaded or the progress state needs to be tracked
                    if (
                        chapters.all { it.chapterImages.isEmpty() } &&
                        manga.progressState == ProgressState.NotStarted
                    ) {
                        // delete if above is true
                        savedMangaDao.deleteSavedManga(manga)
                        coverArtManager.deleteCover(manga.coverArt)
                    } else {
                        // need the saved manga to track progress and save the images
                        savedMangaDao.updateSavedManga(
                            manga.copy(bookmarked = false)
                        )
                    }
                }
            }
                ?: run {
                    getMangaResourceById(id).maxBy { it.first.savedAtLocal }.let { resource ->
                        log("No Saved found using resource $id")
                        saveManga(id) {
                            it.copy(bookmarked = true)
                        }
                        log("Inserted Saved manga using resource $id and set bookmarked true")
                    }
                }
    }

    override suspend fun saveManga(
        id: String,
        copy: ((SavedMangaEntity) -> SavedMangaEntity)?
    ): Unit = withContext(dispatchers.io) {
        getMangaResourceById(id)
            .maxByOrNull { it.first.savedAtLocal }
            ?.let { (resource, _) ->
                val entity = copy?.invoke(SavedMangaEntity(resource)) ?: SavedMangaEntity(resource)
                savedMangaDao.upsertSavedManga(entity)
                log("Inserted Saved manga using resource $id")
                updateChapterList(id)
                coverArtManager.saveCover(id, entity.originalCoverArtUrl)
            }
    }


    override fun getSavedMangaWithChapters(): Flow<List<SavedMangaWithChapters>> {
        return savedMangaDao.getSavedMangaWithChapters()
    }

    override fun getSavedMangaWithChapter(id: String): Flow<SavedMangaWithChapters?> {
        return savedMangaDao.getSavedMangaWithChaptersById(id)
    }

    override fun getSavedMangas(): Flow<List<SavedMangaEntity>> {
        return savedMangaDao.getSavedManga()
    }

    override fun getSavedManga(id: String): Flow<SavedMangaEntity?> {
        return savedMangaDao.getSavedMangaById(id)
    }

    private val syncer = createSyncer<SavedMangaEntity, Manga, String>(
            networkToKey = { manga -> manga.id },
            mapper = { network, saved ->
                saved?.let { mangaUpdateRepository.createUpdate(saved, network) }
                network.toSavedManga(saved)
            },
            upsert = {
                savedMangaDao.upsertSavedManga(it)
                if (it.coverArt.isBlank()) {
                    coverArtManager.saveCover(it.id, it.originalCoverArtUrl)
                    updateChapterList(it.id)
                }
            }
        )

    override suspend fun sync(): Boolean {

        val saved = savedMangaDao.getSavedManga().firstOrNull() ?: emptyList()

        return syncUsing(
            syncer = syncer,
            getCurrent = { saved },
            getNetwork = {
                saved.mapNotNull { saved ->
                    saved.takeIf {
                        saved.status != Status.cancelled && saved.status != Status.completed &&
                                localDateTimeNow().date.toEpochDays() - saved.savedAtLocal.date.toEpochDays() >= 1
                    }
                }
                    .chunked(100)
                    .flatMap {
                        mangaDexApi.getMangaList(
                            MangaRequest(
                                ids = it.map { saved -> saved.id },
                                includes = listOf("cover_art", "author", "artist")
                            )
                        )
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
}

