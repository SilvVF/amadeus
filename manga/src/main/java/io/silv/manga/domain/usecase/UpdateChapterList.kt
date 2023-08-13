package io.silv.manga.domain.usecase

import ChapterListResponse
import android.util.Log
import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.domain.ChapterToChapterEntityMapper
import io.silv.manga.domain.suspendRunCatching
import io.silv.manga.local.dao.ChapterDao
import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.local.entity.syncerForEntity
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.chapter.Chapter
import io.silv.manga.network.mangadex.requests.MangaFeedRequest
import io.silv.manga.network.mangadex.requests.Order
import io.silv.manga.network.mangadex.requests.OrderBy
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

internal class UpdateChapterList(
    private val mangaDexApi: MangaDexApi,
    private val chapterDao: ChapterDao,
    private val dispatchers: AmadeusDispatchers
) {
    private val syncer = syncerForEntity<ChapterEntity, Chapter, String>(
        networkToKey = { it.id },
        mapper = { n, l ->
            ChapterToChapterEntityMapper.map(n to l)
        },
        upsert = {
            chapterDao.upsertChapter(it)
        }
    )

    suspend operator fun invoke(id: String) { updateFromNetwork(id) }

    private suspend fun getInitialChapterList(mangaId: String, langs: List<String> = listOf("en")) = suspendRunCatching {
        mangaDexApi.getMangaFeed(
            mangaId,
            MangaFeedRequest(
                translatedLanguage = langs,
                offset = 0,
                limit = 500,
                includes = listOf("scanlation_group", "user"),
                order = mapOf(Order.chapter to OrderBy.asc)
            )
        )
            .getOrThrow()
    }

    private suspend fun getRestOfChapters(response: ChapterListResponse, mangaId: String, langs: List<String> = listOf("en")): List<Chapter> {
        return withContext(dispatchers.io) {
            val count = (response.total / response.limit)
            (1..count).map {
                mangaDexApi.getMangaFeed(
                    mangaId,
                    MangaFeedRequest(
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