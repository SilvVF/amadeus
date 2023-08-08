package io.silv.manga.domain.repositorys

import android.util.Log
import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.domain.MangaEntityMapper
import io.silv.manga.domain.subtract
import io.silv.manga.domain.timeNow
import io.silv.manga.domain.usecase.GetMangaResourcesById
import io.silv.manga.domain.usecase.UpdateChapterWithArt
import io.silv.manga.local.dao.ChapterDao
import io.silv.manga.local.dao.SavedMangaDao
import io.silv.manga.local.entity.ProgressState
import io.silv.manga.local.entity.SavedMangaEntity
import io.silv.manga.local.entity.relations.SavedMangaWithChapters
import io.silv.manga.local.entity.syncerForEntity
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.Status
import io.silv.manga.network.mangadex.models.manga.Manga
import io.silv.manga.network.mangadex.requests.MangaRequest
import io.silv.manga.sync.Synchronizer
import io.silv.manga.sync.syncWithSyncer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Duration
import kotlin.time.toKotlinDuration

internal class SavedMangaRepositoryImpl(
    private val savedMangaDao: SavedMangaDao,
    private val getMangaResourceById: GetMangaResourcesById,
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
        upsert = { savedMangaDao.upsertSavedManga(it) }
    )

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
                        val entity =  SavedMangaEntity(resource.first).copy(bookmarked = true)
                        savedMangaDao.upsertSavedManga(entity)
                        log("Inserted Saved manga using resource $id and set bookmarked true")
                    }
                }
    }

    override suspend fun saveManga(id: String): Unit = withContext(dispatchers.io) {
        getMangaResourceById(id).maxBy { it.first.savedAtLocal }.let { resource ->
            log("No Saved found using resource $id")
            val entity =  SavedMangaEntity(resource.first)
            savedMangaDao.upsertSavedManga(entity)
            log("Inserted Saved manga using resource $id and set bookmarked true")
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

    override suspend fun updateLastReadPage(mangaId: String, chapterId: String, page: Int) {
        withContext(dispatchers.io) {
            val chapter = chapterDao.getChapterById(chapterId).first() ?: return@withContext
            chapterDao.updateChapter(
                chapter.copy(
                    progressState = when(page) {
                        chapter.pages -> ProgressState.Finished
                        in 1..chapter.pages -> {
                            if (chapter.progressState != ProgressState.Finished)
                                ProgressState.Reading
                            else
                                ProgressState.Finished
                        }
                        else -> ProgressState.NotStarted
                    }
                )
            )
            savedMangaDao.getSavedMangaById(mangaId).first()?.let { entity ->
                savedMangaDao.updateSavedManga(
                    entity.copy(
                        chapterToLastReadPage = entity.chapterToLastReadPage.toMutableMap().apply {
                            if ((this[chapterId] ?: -1) < page)
                                this[chapterId] = page
                        },
                        readChapters = if (page == chapter.pages) {
                            entity.readChapters + chapterId
                        } else {
                            entity.readChapters
                        }
                    )
                )
            }
        }
    }

    override suspend fun syncWith(synchronizer: Synchronizer): Boolean {
        val saved = savedMangaDao.getSavedManga().first()
        return synchronizer.syncWithSyncer(
            syncer = syncer,
            getCurrent = { saved },
            getNetwork = {
                saved.mapNotNull { saved ->
                    saved.takeIf {
                        timeNow().subtract(saved.savedAtLocal) > Duration.ofDays(4).toKotlinDuration()
                                && saved.status != Status.cancelled
                                && saved.status != Status.completed
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

