package io.silv.domain.chapter

import io.silv.common.AmadeusDispatchers
import io.silv.data.download.DownloadManager
import io.silv.database.dao.ChapterDao
import io.silv.domain.manga.GetSavableManga
import io.silv.model.SavableChapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn

class GetBookmarkedChapters(
    private val chapterDao: ChapterDao,
    private val getManga: GetSavableManga,
    private val downloadManager: DownloadManager,
    private val dispatchers: AmadeusDispatchers
) {

    fun subscribe(): Flow<List<SavableChapter>> =
        chapterDao.observeBookmarkedChapters()
            .combine(downloadManager.cacheChanges) { list, _ ->
                list.map { chapter ->
                    SavableChapter(
                        entity = chapter,
                        downloaded = getManga.await(chapter.mangaId)?.let { manga ->
                            downloadManager.isChapterDownloaded(chapter.title, chapter.scanlator, manga.titleEnglish)
                        }
                            ?: false
                    )
                }
            }
            .flowOn(dispatchers.io)

    suspend fun await() = subscribe().firstOrNull()
}