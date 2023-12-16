package io.silv.data.chapter

import android.util.Log
import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.suspendMapSuccess
import com.skydoves.sandwich.suspendOnFailure
import io.silv.common.coroutine.suspendRunCatching
import io.silv.common.model.ProgressState
import io.silv.common.time.localDateTimeNow
import io.silv.common.time.minus
import io.silv.data.mappers.toChapterEntity
import io.silv.database.entity.chapter.ChapterEntity
import io.silv.network.requests.ChapterListRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.hours


internal class ChapterRepositoryImpl(
    private val chapterDao: io.silv.database.dao.ChapterDao,
    private val mangaDexApi: io.silv.network.MangaDexApi,
    private val updateChapterList: io.silv.data.util.UpdateChapterList,
    private val dispatchers: io.silv.common.AmadeusDispatchers,
): ChapterRepository {

    override suspend fun bookmarkChapter(id: String)  {
        withContext(dispatchers.io) {
            chapterDao.getChapterById(id)?.let {prev ->
                chapterDao.updateChapter(
                    prev.copy(bookmarked = !prev.bookmarked)
                )
            }
        }
    }

    override suspend fun updateChapter(id: String, copy: (ChapterEntity) -> ChapterEntity): ChapterEntity {
        var chapter: ChapterEntity? = null
        withContext(dispatchers.io) {
            chapterDao.getChapterById(id)?.let { prev ->
                chapter = copy(prev)
                chapterDao.updateChapter(chapter!!)
            }
        }
        return chapter!!
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

                response.data.forEach {chapter ->
                    chapterDao.upsertChapter(
                       chapter.toChapterEntity(
                            prev = chapters.find { entity -> entity.id == chapter.id }
                       )
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

    override suspend fun refetchChapter(id: String): ChapterEntity? {
        return withContext(dispatchers.io) {
            mangaDexApi.getChapterData(ChapterListRequest(ids = listOf(id)))
                    .suspendMapSuccess {
                        val chapter = this.data.first().toChapterEntity()
                        chapterDao.upsertChapter(chapter)
                        chapter
                    }
                    .getOrNull()
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
        chapters == null || chapters.any { localDateTimeNow() - (it.savedLocalAt) > 12.hours }
    }

    override fun getChapters(mangaId: String): Flow<List<ChapterEntity>> {
        return chapterDao.observeChaptersByMangaId(mangaId).onStart {
            if (shouldUpdate(mangaId)) {
                Log.d("ChapterEntityRepository","Updating from network")
                updateChapterList(mangaId)
            } else {
                Log.d("ChapterEntityRepository","Skipped Fetch From network")
            }
        }
    }


    override suspend fun sync(): Boolean {
        return true
    }
}
