package io.silv.manga.domain.repositorys

import android.util.Log
import io.silv.core.AmadeusDispatchers
import io.silv.manga.local.dao.MangaResourceDao
import io.silv.manga.local.dao.SavedMangaDao
import io.silv.manga.local.entity.MangaResource
import io.silv.manga.local.entity.ProgressState
import io.silv.manga.local.entity.SavedMangaEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class SavedMangaRepositoryImpl(
    private val savedMangaDao: SavedMangaDao,
    private val mangaDao: MangaResourceDao,
    private val dispatchers: AmadeusDispatchers,
): SavedMangaRepository {

    private val TAG = "SavedMangaRepositoryImpl"
    private fun log(msg: String) = Log.d(TAG, msg)

    override suspend fun bookmarkManga(id: String) {
        CoroutineScope(dispatchers.io).launch {
            savedMangaDao.getMangaById(id)?.let { saved ->
                val bookmarked = !saved.bookmarked
                log("found saved manga $id updating bookmarked from ${!bookmarked} to $bookmarked")
                if (bookmarked) {
                    savedMangaDao.updateSavedManga(saved.copy(bookmarked = true))
                } else {
                    if (hasBeenStartedOrDataDownloaded(saved)) {
                        savedMangaDao.updateSavedManga(saved.copy(bookmarked = false))
                    } else {
                        log("deleted unused saved manga $id")
                        savedMangaDao.delete(saved)
                    }
                }
            } ?:
            mangaDao.getMangaById(id)?.let { resource ->
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

    private fun hasBeenStartedOrDataDownloaded(saved: SavedMangaEntity): Boolean {
        return (
                saved.chaptersIds.isNotEmpty() &&
                saved.progressState != ProgressState.NotStarted
        )
            .also { log("Manga ${saved.id} has been hasBeenStartedOrDataDownloaded = $it") }
    }

    override fun getSavedManga(): Flow<List<SavedMangaEntity>> {
        return savedMangaDao.getAllAsFlow()
    }
}