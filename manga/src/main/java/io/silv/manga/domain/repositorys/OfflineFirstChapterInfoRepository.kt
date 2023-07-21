package io.silv.manga.domain.repositorys

import io.silv.core.AmadeusDispatchers
import io.silv.manga.domain.usecase.GetMangaResourceById
import io.silv.manga.domain.usecase.UpdateChapterWithArt
import io.silv.manga.domain.usecase.UpdateInfo
import io.silv.manga.domain.usecase.UpdateResourceChapterWithArt
import io.silv.manga.domain.usecase.UpdateResourceInfo
import io.silv.manga.local.dao.ChapterDao
import io.silv.manga.local.dao.SavedMangaDao
import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.sync.Synchronizer
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus


internal class OfflineFirstChapterInfoRepository(
    private val chapterDao: ChapterDao,
    private val mangaDexApi: MangaDexApi,
    private val savedMangaDao: SavedMangaDao,
    private val getMangaResourceById: GetMangaResourceById,
    private val updateChapter: UpdateChapterWithArt,
    private val updateResourceChapter: UpdateResourceChapterWithArt,
    dispatchers: AmadeusDispatchers,
): ChapterInfoRepository {

    private val scope = CoroutineScope(dispatchers.io) +
            CoroutineName("OfflineFirstChapterInfoRepository")

    override val loadingIds = MutableStateFlow(emptyList<String>())

    override fun getChapters(mangaId: String): Flow<List<ChapterEntity>> {
        return chapterDao.getChaptersByMangaId(mangaId).onEach { entities ->
            if (entities.isEmpty()) {
                updateChapterList(mangaId)
            }
        }
    }

    private fun updateChapterList(mangaId: String) = scope.launch {
        loadingIds.update { it + mangaId }

        val savedManga = savedMangaDao.getMangaById(mangaId)
        val (mangaResource, daoId) = getMangaResourceById(mangaId)

        if (savedManga != null) {
            updateChapter(
                UpdateInfo(
                    mangaId,
                    chapterDao,
                    savedMangaDao,
                    mangaDexApi,
                    savedManga
                )
            )
        } else if (mangaResource != null) {
            updateResourceChapter(
                UpdateResourceInfo(
                    id = mangaId,
                    chapterDao = chapterDao,
                    mangaDexApi = mangaDexApi,
                    daoId = daoId,
                    mangaResource = mangaResource
                )
            )
        }
    }
        .invokeOnCompletion {
            loadingIds.update { ids -> ids.filterNot { it == mangaId } }
        }


    override suspend fun syncWith(synchronizer: Synchronizer): Boolean {
        val savedManga = savedMangaDao.getAll()
        val savedChapters = chapterDao.getAll()
        // Delete any chapter that is not associated with a saved manga
        // and that has no chapter images downloaded
        val savedChaptersAfterDeletion = savedChapters.filter { chapter ->
            savedManga.none { it.id == chapter.id } && chapter.chapterImages.isEmpty()
                .also { unused ->
                    if (unused) chapterDao.deleteChapter(chapter)
                }
        }

        return runCatching {
            for (chapterEntity in savedChaptersAfterDeletion) {
                updateChapter(
                    UpdateInfo(
                        chapterEntity.mangaId,
                        chapterDao,
                        savedMangaDao,
                        mangaDexApi,
                        savedManga.find { it.id == chapterEntity.mangaId } ?: continue
                    )
                )
            }
        }
            .isSuccess
    }
}
