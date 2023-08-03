package io.silv.manga.domain.repositorys.chapter

import android.util.Log
import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.ktor_response_mapper.message
import io.silv.ktor_response_mapper.suspendOnFailure
import io.silv.ktor_response_mapper.suspendOnSuccess
import io.silv.manga.domain.coverArtUrl
import io.silv.manga.domain.suspendRunCatching
import io.silv.manga.domain.usecase.GetMangaResourcesById
import io.silv.manga.domain.usecase.UpdateChapterWithArt
import io.silv.manga.domain.usecase.UpdateInfo
import io.silv.manga.domain.usecase.UpdateMangaResourceWithArt
import io.silv.manga.local.dao.ChapterDao
import io.silv.manga.local.dao.SavedMangaDao
import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.ContentRating
import io.silv.manga.network.mangadex.models.chapter.Chapter
import io.silv.manga.network.mangadex.requests.CoverArtRequest
import io.silv.manga.network.mangadex.requests.MangaFeedRequest
import io.silv.manga.network.mangadex.requests.Order
import io.silv.manga.network.mangadex.requests.OrderBy
import io.silv.manga.sync.Synchronizer
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.plus
import kotlin.math.ceil
import kotlin.math.roundToInt




internal class OfflineFirstChapterInfoRepository(
    private val chapterDao: ChapterDao,
    private val mangaDexApi: MangaDexApi,
    private val savedMangaDao: SavedMangaDao,
    private val updateChapter: UpdateChapterWithArt,
    dispatchers: AmadeusDispatchers,
): ChapterEntityRepository {

    private val scope = CoroutineScope(dispatchers.io) + CoroutineName("OfflineFirstChapterInfoRepository")

    override fun getChapters(mangaId: String): Flow<List<ChapterEntity>> {
        return chapterDao.getChaptersByMangaId(mangaId)
    }

    override suspend fun syncWith(synchronizer: Synchronizer): Boolean {
        val savedManga = savedMangaDao.getSavedManga().first()
        val savedChapters = chapterDao.getChapterEntities().first()
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
                        id = chapterEntity.mangaId,
                        chapterDao = chapterDao,
                        savedMangaDao = savedMangaDao,
                        mangaDexApi = mangaDexApi,
                        entity = savedManga.find { it.id == chapterEntity.mangaId } ?: continue,
                        page = 0,
                        fetchLatest = true
                    )
                )
            }
        }
            .isSuccess
    }
}
