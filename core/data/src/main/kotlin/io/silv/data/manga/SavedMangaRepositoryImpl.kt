package io.silv.data.manga

import android.util.Log
import com.skydoves.sandwich.getOrThrow
import io.silv.common.model.Status
import io.silv.common.time.localDateTimeNow
import io.silv.data.mappers.toSavedManga
import io.silv.data.util.UpdateChapterList
import io.silv.data.util.createSyncer
import io.silv.data.util.syncUsing
import io.silv.database.dao.SavedMangaDao
import io.silv.database.dao.SourceMangaDao
import io.silv.database.entity.manga.SavedMangaEntity
import io.silv.database.entity.relations.SavedMangaWithChapters
import io.silv.network.model.manga.Manga
import io.silv.network.requests.MangaRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

internal class SavedMangaRepositoryImpl(
    private val savedMangaDao: SavedMangaDao,
    private val sourceMangaDao: SourceMangaDao,
    private val mangaDexApi: io.silv.network.MangaDexApi,
    private val dispatchers: io.silv.common.AmadeusDispatchers,
    private val updateChapterList: UpdateChapterList,
    private val mangaUpdateRepository: MangaUpdateRepository
): SavedMangaRepository {

    private val TAG = "SavedMangaRepositoryImpl"

    private fun log(msg: String) = Log.d(TAG, msg)

    override suspend fun addOrRemoveFromLibrary(id: String): Unit = withContext(dispatchers.io) {
        val saved = savedMangaDao.getSavedMangaById(id).firstOrNull()

        if (saved == null) {
            val sourceManga = sourceMangaDao.getById(id) ?: return@withContext
            savedMangaDao.upsertSavedManga(SavedMangaEntity(sourceManga))
        } else {
            savedMangaDao.deleteSavedManga(saved)
        }
    }

    override suspend fun saveManga(
        id: String,
        block: (SavedMangaEntity) -> SavedMangaEntity
    ): Boolean = withContext(dispatchers.io) {
        sourceMangaDao.getById(id)?.let { resource ->
            val saved = block(SavedMangaEntity(resource))

            savedMangaDao.upsertSavedManga(saved)
            updateChapterList(saved.id)

            true
        }
            ?: false
    }


    override fun observeSavedMangaListWithChapters(): Flow<List<SavedMangaWithChapters>> {
        return savedMangaDao.getSavedMangaWithChapters()
    }

    override fun observeSavedMangaWithChaptersById(id: String): Flow<SavedMangaWithChapters?> {
        return savedMangaDao.getSavedMangaWithChaptersById(id)
    }

    override fun observeSavedMangaList(): Flow<List<SavedMangaEntity>> {
        return savedMangaDao.getSavedManga()
    }

    override fun observeSavedMangaById(id: String): Flow<SavedMangaEntity?> {
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
                updateChapterList(it.id)
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

