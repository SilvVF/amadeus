package io.silv.manga.domain.repositorys

import android.util.Log
import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.domain.MangaEntityMapper
import io.silv.manga.domain.usecase.UpdateChapterWithArt
import io.silv.manga.domain.usecase.UpdateInfo
import io.silv.manga.local.dao.ChapterDao
import io.silv.manga.local.dao.MangaResourceDao
import io.silv.manga.local.dao.SavedMangaDao
import io.silv.manga.local.entity.ProgressState
import io.silv.manga.local.entity.SavedMangaEntity
import io.silv.manga.local.entity.relations.MangaWithChapters
import io.silv.manga.local.entity.syncerForEntity
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.manga.Manga
import io.silv.manga.network.mangadex.requests.MangaByIdRequest
import io.silv.manga.sync.Synchronizer
import io.silv.manga.sync.syncWithSyncer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

internal class SavedMangaRepositoryImpl(
    private val savedMangaDao: SavedMangaDao,
    private val mangaDao: MangaResourceDao,
    private val mangaDexApi: MangaDexApi,
    private val dispatchers: AmadeusDispatchers,
    private val chapterDao: ChapterDao,
    private val updateChapter: UpdateChapterWithArt
): SavedMangaRepository {

    private val TAG = "SavedMangaRepositoryImpl"
    private fun log(msg: String) = Log.d(TAG, msg)

    private val mapper = MangaEntityMapper

    private val syncer = syncerForEntity<SavedMangaEntity, Manga, String>(
        networkToKey = { manga -> manga.id },
        mapper = { network, saved -> mapper.map(network to saved) },
        upsert = { savedMangaDao.upsertManga(it) }
    )

    override suspend fun bookmarkManga(id: String): Unit = withContext(dispatchers.io) {
            savedMangaDao.getMangaWithChapters(id)?.let { (manga, chapters) ->
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
                        savedMangaDao.delete(manga)
                    } else {
                        // need the saved manga to track progress and save the images
                        savedMangaDao.updateSavedManga(
                            manga.copy(bookmarked = false)
                        )
                    }
                }
            } ?: saveManga(id)
    }

    override suspend fun saveManga(id: String) {
        mangaDao.getMangaById(id)?.let { resource ->
            log("No Saved found using resource $id")
            val entity =  SavedMangaEntity(resource).copy(
                bookmarked = true
            )
            savedMangaDao.upsertManga(entity)
            updateChapter(
                UpdateInfo(id, chapterDao, savedMangaDao, mangaDexApi, entity)
            )
            log("Inserted Saved manga using resource $id and set bookmarked true")
        }
    }


    override fun getSavedMangaWithChapters(): Flow<List<MangaWithChapters>> {
        return savedMangaDao.getAllMangaWithChaptersAsFlow()
    }

    override fun getSavedMangaWithChapter(id: String): Flow<MangaWithChapters?> {
        return savedMangaDao.getMangaWithChaptersAsFlow(id)
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
                savedMangaDao.getAll().mapNotNull {
                    delay(1000)
                    if (
                        Clock.System.now().epochSeconds - it.savedLocalAtEpochSeconds > 60 * 60 * 12
                    ) {
                        mangaDexApi.getMangaById(
                            it.id,
                            MangaByIdRequest(includes = listOf("cover_art"))
                        )
                            .getOrThrow()
                            .data
                    } else {
                        null
                    }
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

