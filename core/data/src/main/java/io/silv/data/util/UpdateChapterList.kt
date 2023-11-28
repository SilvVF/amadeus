package io.silv.data.util

import ChapterListResponse
import android.util.Log
import io.silv.common.AmadeusDispatchers
import io.silv.common.coroutine.suspendRunCatching
import io.silv.data.mappers.toChapterEntity
import io.silv.database.dao.ChapterDao
import io.silv.database.entity.chapter.ChapterEntity
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.network.MangaDexApi
import io.silv.network.model.chapter.Chapter
import io.silv.network.requests.MangaFeedRequest
import io.silv.network.requests.Order
import io.silv.network.requests.OrderBy
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

internal class UpdateChapterList(
    private val mangaDexApi: MangaDexApi,
    private val chapterDao: ChapterDao,
    private val dispatchers: AmadeusDispatchers
) {
    private val syncer = createSyncer<ChapterEntity, Chapter, String>(
            networkToKey = { it.id },
            mapper = { chapter, entity ->
                chapter.toChapterEntity(entity)
            },
            upsert = {
                chapterDao.upsertChapter(it)
            }
        )

    suspend operator fun invoke(id: String) { updateFromNetwork(id) }

    private suspend fun getInitialChapterList(
        mangaId: String,
        langs: List<String> = listOf("en")
    ) = suspendRunCatching {
        mangaDexApi.getMangaFeed(
            mangaId = mangaId,
            mangaFeedRequest = MangaFeedRequest(
                translatedLanguage = langs,
                offset = 0,
                limit = 500,
                includes = listOf("scanlation_group", "user"),
                order = mapOf(Order.chapter to OrderBy.asc)
            )
        )
            .getOrThrow()
    }

    private suspend fun getRestOfChapters(
        response: ChapterListResponse,
        mangaId: String,
        langs: List<String> = listOf("en")
    ): List<Chapter> {
        return withContext(dispatchers.io) {
            val count = (response.total / response.limit)
            (1..count).map {
                mangaDexApi.getMangaFeed(
                    mangaId = mangaId,
                    mangaFeedRequest = MangaFeedRequest(
                        translatedLanguage = langs,
                        offset = it * response.limit,
                        limit = 500,
                        includes = listOf("scanlation_group", "user"),
                        order = mapOf(Order.chapter to OrderBy.asc)
                    )
                )
                    .getOrThrow()
                    .data
            }
                .flatten()
        }
    }

    private suspend fun updateFromNetwork(mangaId: String) = withContext(dispatchers.io) {
        getInitialChapterList(mangaId)
            .fold(
                onSuccess = {
                    val result = syncer.sync(
                        current = chapterDao.observeChaptersByMangaId(mangaId).firstOrNull() ?: emptyList(),
                        networkResponse =  it.data + getRestOfChapters(it, mangaId)
                    )
                    for (chapter in result.unhandled) {
                        if (!chapter.downloaded) {
                            Log.d("ChapterEntityRepository", "deleted ${chapter.id}")
                            chapterDao.deleteChapter(chapter)
                        }
                    }
                },
                onFailure = {
                    it.printStackTrace()
                }
            )
    }
}