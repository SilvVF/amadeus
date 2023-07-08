package io.silv.manga.domain.repositorys

import android.util.Log
import io.silv.core.AmadeusDispatchers
import io.silv.core.Mapper
import io.silv.ktor_response_mapper.getOrNull
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.domain.ChapterToChapterEntityMapper
import io.silv.manga.domain.MangaEntityMapper
import io.silv.manga.domain.alternateTitles
import io.silv.manga.domain.coverArtUrl
import io.silv.manga.domain.descriptionEnglish
import io.silv.manga.domain.titleEnglish
import io.silv.manga.local.dao.ChapterDao
import io.silv.manga.local.dao.MangaResourceDao
import io.silv.manga.local.dao.SavedMangaDao
import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.local.entity.ProgressState
import io.silv.manga.local.entity.SavedMangaEntity
import io.silv.manga.local.entity.relations.MangaWithChapters
import io.silv.manga.local.entity.syncerForEntity
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.manga.Manga
import io.silv.manga.network.mangadex.requests.CoverArtRequest
import io.silv.manga.network.mangadex.requests.MangaByIdRequest
import io.silv.manga.sync.Synchronizer
import io.silv.manga.sync.syncWithSyncer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class SavedMangaRepositoryImpl(
    private val savedMangaDao: SavedMangaDao,
    private val mangaDao: MangaResourceDao,
    private val mangaDexApi: MangaDexApi,
    private val dispatchers: AmadeusDispatchers,
    private val chapterDao: ChapterDao
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
            } ?: mangaDao.getMangaById(id)?.let { resource ->
                log("No Saved found using resource $id")
                val entity =  SavedMangaEntity(resource).copy(
                    bookmarked = true
                )
                savedMangaDao.upsertManga(entity)
                launch {
                    updateVolumeCoverArtAndChapterInfo(id, entity)
                }
                log("Inserted Saved manga using resource $id and set bookmarked true")
            }
    }
    // Load chapters info that will also attach volume images
    // tries to preload the data and will fail if there is no internet
    // this will be fetched later in any screen that needs it or during sync
    private suspend fun updateVolumeCoverArtAndChapterInfo(id: String, entity: SavedMangaEntity) {
        runCatching {
            mangaDexApi.getMangaFeed(id)
                .getOrNull()
                ?.let { chapterListResponse ->
                    chapterListResponse.data.forEach {
                        chapterDao.upsertChapter(
                            ChapterToChapterEntityMapper.map(it to null)
                        )
                    }
                }
            mangaDexApi.getCoverArtList(CoverArtRequest(manga = listOf(id)))
                   .getOrNull()?.let { r ->
                        savedMangaDao.updateSavedManga(
                            entity.copy(
                                volumeToCoverArt = buildMap {
                                    r.data.forEach { cover ->
                                        put(
                                            cover.attributes.volume ?: "0",
                                            coverArtUrl(cover.attributes.fileName, id)
                                        )
                                    }
                                }
                            )
                        )
                   }
        }
    }

    override fun getSavedMangaWithChapters(): Flow<List<MangaWithChapters>> {
        return savedMangaDao.getAllMangaWithChaptersAsFlow()
    }

    override fun getSavedMangaWithChapter(id: String): Flow<MangaWithChapters?> {
        return savedMangaDao.getMangaWithChaptersAsFlow(id).onEach {
            if (it == null) {
                CoroutineScope(dispatchers.io).launch {
                    mangaDao.getMangaById(id)?.let { resource ->
                        log("No Saved found using resource $id")
                        val entity =  SavedMangaEntity(resource).copy(
                            bookmarked = true
                        )
                        savedMangaDao.upsertManga(entity)
                        launch {
                            updateVolumeCoverArtAndChapterInfo(id, entity)
                        }
                        log("Inserted Saved manga using resource $id and set bookmarked true")
                    }
                }
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
                    delay(1000)
                    mangaDexApi.getMangaById(
                        it.id,
                        MangaByIdRequest(includes = listOf("cover_art"))
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