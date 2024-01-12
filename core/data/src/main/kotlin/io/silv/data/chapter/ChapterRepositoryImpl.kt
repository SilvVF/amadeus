package io.silv.data.chapter

import android.util.Log
import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.suspendMapSuccess
import com.skydoves.sandwich.suspendOnFailure
import io.silv.common.AmadeusDispatchers
import io.silv.common.coroutine.suspendRunCatching
import io.silv.common.time.localDateTimeNow
import io.silv.common.time.minus
import io.silv.data.mappers.toChapterEntity
import io.silv.data.util.UpdateChapterList
import io.silv.database.dao.ChapterDao
import io.silv.domain.chapter.model.Chapter
import io.silv.domain.chapter.repository.ChapterRepository
import io.silv.network.MangaDexApi
import io.silv.network.requests.ChapterListRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.hours


internal class ChapterRepositoryImpl(
    private val chapterDao: ChapterDao,
    private val mangaDexApi: MangaDexApi,
    private val updateChapterList: UpdateChapterList,
    private val dispatchers: AmadeusDispatchers,
): ChapterRepository {

    override suspend fun bookmarkChapter(id: String)  {
        withContext(dispatchers.io) {
            chapterDao.getChapterById(id)?.let { prev ->
                chapterDao.updateChapter(
                    prev.copy(bookmarked = !prev.bookmarked)
                )
            }
        }
    }

    override suspend fun updateChapter(id: String, copy: (Chapter) -> Chapter): Chapter? {
        return withContext(dispatchers.io) {
            chapterDao.getChapterById(id)?.let { prev ->
                val chapter = copy(prev.let(ChapterMapper::mapChapter))
                chapterDao.updateChapter(chapter.let(ChapterMapper::toEntity))
                chapter
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

    override suspend fun saveChapter(chapter: Chapter) {
        withContext(dispatchers.io) {
            chapterDao.upsertChapter(chapter.let(ChapterMapper::toEntity))
        }
    }

    override suspend fun refetchChapter(id: String): Chapter? {
        return withContext(dispatchers.io) {
            mangaDexApi.getChapterData(ChapterListRequest(ids = listOf(id)))
                    .suspendMapSuccess {
                        val chapter = this.data.first().toChapterEntity()
                        chapterDao.upsertChapter(chapter)
                        chapter.let(ChapterMapper::mapChapter)
                    }
                    .getOrNull()
        }
    }

    override suspend fun getChapterById(id: String): Chapter? {
        return chapterDao.getChapterById(id)?.let(ChapterMapper::mapChapter)
    }

    override fun observeChapterById(id: String): Flow<Chapter> {
        return chapterDao.observeChapterById(id).filterNotNull().map(ChapterMapper::mapChapter)
    }

    override fun observeChapters(): Flow<List<Chapter>> {
        return chapterDao.getChapterEntities()
            .map { list -> list.map(ChapterMapper::mapChapter) }
            .flowOn(dispatchers.io)
    }

    override fun observeBookmarkedChapters(): Flow<List<Chapter>> {
        return chapterDao.observeBookmarkedChapters().map { it.map(ChapterMapper::mapChapter) }.flowOn(dispatchers.io)
    }

    private suspend fun shouldUpdate(mangaId: String): Boolean = withContext(dispatchers.io) {
        val chapters = chapterDao.getChaptersByMangaId(mangaId).ifEmpty { null }
        chapters == null || chapters.any { localDateTimeNow() - (it.savedLocalAt) > 12.hours }
    }

    override fun observeChaptersByMangaId(mangaId: String): Flow<List<Chapter>> {
        return chapterDao.observeChaptersByMangaId(mangaId).map { list -> list.map(ChapterMapper::mapChapter) }.onStart {
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
