package io.silv.manga.domain.repositorys

import io.silv.core.AmadeusDispatchers
import io.silv.manga.local.dao.MangaResourceDao
import io.silv.manga.local.dao.SavedMangaDao
import io.silv.manga.local.entity.SavedMangaEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

internal class SavedMangaRepositoryImpl(
    private val savedMangaDao: SavedMangaDao,
    private val mangaDao: MangaResourceDao,
    private val dispatchers: AmadeusDispatchers,
): SavedMangaRepository {

    override suspend fun bookmarkManga(id: String) {
        CoroutineScope(dispatchers.io).launch {
            savedMangaDao.getMangaById(id)?.let { saved ->
                println("Updating saved manga $saved")
                savedMangaDao.updateSavedManga(
                    saved.copy(bookmarked = !saved.bookmarked)
                )
            } ?:
            mangaDao.getMangaById(id)?.let { resource ->
                println("Inserting to saved manga $resource")
                savedMangaDao.upsertManga(
                    SavedMangaEntity(resource).copy(
                        bookmarked = true
                    )
                )
            }
        }
    }

    override fun getSavedManga(): Flow<List<SavedMangaEntity>> {
        return savedMangaDao.getAllAsFlow()
    }
}