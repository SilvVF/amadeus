package io.silv.manga.domain.repositorys.chapter

import android.util.Log
import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.ktor_response_mapper.suspendOnFailure
import io.silv.manga.domain.ChapterToChapterEntityMapper
import io.silv.manga.domain.coverArtUrl
import io.silv.manga.domain.minus
import io.silv.manga.domain.suspendRunCatching
import io.silv.manga.domain.timeNow
import io.silv.manga.domain.usecase.GetMangaResourcesById
import io.silv.manga.domain.usecase.UpdateChapterList
import io.silv.manga.domain.usecase.UpdateMangaResourceWithArt
import io.silv.manga.local.dao.ChapterDao
import io.silv.manga.local.dao.SavedMangaDao
import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.local.entity.ProgressState
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.requests.ChapterListRequest
import io.silv.manga.network.mangadex.requests.CoverArtRequest
import io.silv.manga.sync.Synchronizer
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import kotlin.time.toKotlinDuration


internal class OfflineFirstChapterInfoRepository(
    private val chapterDao: ChapterDao,
    private val mangaDexApi: MangaDexApi,
    private val savedMangaDao: SavedMangaDao,
    private val getMangaResourcesById: GetMangaResourcesById,
    private val updateVolumeWithCoverArt: UpdateMangaResourceWithArt,
    private val updateChapterList: UpdateChapterList,
    private val dispatchers: AmadeusDispatchers,
): ChapterEntityRepository {

    private val scope = CoroutineScope(dispatchers.io) + CoroutineName("OfflineFirstChapterInfoRepository")

    override val loadingVolumeArtIds = MutableStateFlow(emptyList<String>())

    override suspend fun bookmarkChapter(id: String)  {
        withContext(dispatchers.io) {
            chapterDao.getChapterById(id)?.let {prev ->
                chapterDao.updateChapter(
                    prev.copy(bookmarked = !prev.bookmarked)
                )
            }
        }
    }

    override suspend fun updateChapter(id: String, copy: (ChapterEntity) -> ChapterEntity) {
        withContext(dispatchers.io) {
            chapterDao.getChapterById(id)?.let { prev ->
                chapterDao.updateChapter(
                    copy(prev)
                )
            }
        }
    }

    override suspend fun updateLastReadPage(chapterId: String, page: Int, lastPage: Int) {
        withContext(dispatchers.io) {
            chapterDao.getChapterById(chapterId)?.let {
                chapterDao.updateChapter(
                    it.copy(
                        lastPageRead = page.coerceAtMost(lastPage),
                        pages = lastPage,
                        progressState = if (page >= lastPage) ProgressState.Finished else ProgressState.Reading
                    )
                )
            }
        }
    }

    override suspend fun saveChapters(ids: List<String>): Boolean {
        return suspendRunCatching {
            withContext(dispatchers.io) {
                val chapterJobs = ids.map { async { chapterDao.getChapterById(it) } }
                val response = mangaDexApi.getChapterData(ChapterListRequest(ids = ids))
                    .suspendOnFailure { Log.d("OfflineFirstChapterInfoRepository", "fetching failed $ids") }
                    .getOrThrow()
                val chapters = chapterJobs.mapNotNull { it.await() }
                response.data.forEach {
                    chapterDao.upsertChapter(
                        ChapterToChapterEntityMapper.map(it to chapters.find { c -> c.id == it.id })
                    )
                }
            }
        }
            .isSuccess
    }

    override suspend fun saveChapter(chapterEntity: ChapterEntity) {
        withContext(dispatchers.io) {
            chapterDao.upsertChapter(chapterEntity)
        }
    }

    override fun getChapterById(id: String): Flow<ChapterEntity?> {
        return chapterDao.observeChapterById(id).flowOn(dispatchers.io)
    }

    override fun getAllChapters(): Flow<List<ChapterEntity>> {
        return chapterDao.getChapterEntities().flowOn(dispatchers.io)
    }

    private suspend fun shouldUpdate(mangaId: String): Boolean = withContext(dispatchers.io) {
        val chapters = chapterDao.getChaptersByMangaId(mangaId).ifEmpty { null }
        chapters == null || chapters.any {
            timeNow().minus(it.savedLocalAt) > java.time.Duration.ofHours(12).toKotlinDuration()
        }
    }

    override fun getChapters(mangaId: String): Flow<List<ChapterEntity>> {
        return chapterDao.observeChaptersByMangaId(mangaId).onStart {
            if (shouldUpdate(mangaId)) {
                Log.d("ChapterEntityRepository","Updating from network")
                updateChapterList(mangaId)
                updateVolumeArt(mangaId)
            } else {
                Log.d("ChapterEntityRepository","Skipped Fetch From network")
            }
        }
    }


    private suspend fun updateVolumeArt(mangaId: String) = withContext(scope.coroutineContext) {
        suspendRunCatching {

            loadingVolumeArtIds.update { it + mangaId }

            val savedManga = savedMangaDao.getSavedMangaById(mangaId).first()
            val resourceToIdList = getMangaResourcesById(mangaId).also {
                Log.d("ChapterInfoRepositoryImpl", it.size.toString() + "resources")
            }

            val list = mangaDexApi.getCoverArtList(
                CoverArtRequest(
                    manga = listOf(mangaId),
                    limit = 100,
                    offset = 0
                )
            )
                .getOrThrow()

            val volumeCoverArt = buildMap {
                list.data.forEach { cover ->
                    put(
                        cover.attributes.volume ?: "0",
                        coverArtUrl(cover.attributes.fileName, mangaId)
                    )
                }
            }

            if (savedManga != null) {
                savedMangaDao.updateSavedManga(
                    savedManga.copy(
                        volumeToCoverArt = savedManga.volumeToCoverArt + volumeCoverArt
                    )
                )
            }
            resourceToIdList.forEach { (r, id) ->
                Log.d("ChapterInfoRepositoryImpl", id.toString() + "trying to update")
                updateVolumeWithCoverArt(id, r, volumeCoverArt + r.volumeToCoverArt)
            }
        }
            .onSuccess {
                loadingVolumeArtIds.update { it - mangaId }
            }
            .onFailure { e ->
                Log.d("ChapterInfoRepositoryImpl", e.message ?: e.stackTraceToString())
                loadingVolumeArtIds.update { it - mangaId }
            }
    }

    override suspend fun syncWith(synchronizer: Synchronizer): Boolean {

//        val savedManga = savedMangaDao.getSavedManga().first()
//        val savedChapters = chapterDao.getChapterEntities().first()
//            .filter {
//                // filter chapters that could have had an update
//                it.savedLocalAt.minus(it.readableAt).inWholeSeconds >= 0
//            }
//
//        return runCatching {
//            for (chapterEntity in savedChapters) {
//                updateChapter(
//                    UpdateInfo(
//                        id = chapterEntity.mangaId,
//                        chapterDao = chapterDao,
//                        savedMangaDao = savedMangaDao,
//                        mangaDexApi = mangaDexApi,
//                        entity = savedManga.find { it.id == chapterEntity.mangaId } ?: continue,
//                        page = 0,
//                        fetchLatest = true
//                    )
//                )
//            }
//        }
//            .isSuccess
        return true
    }
}
