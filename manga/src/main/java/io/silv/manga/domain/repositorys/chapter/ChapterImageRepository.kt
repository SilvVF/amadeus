package io.silv.manga.domain.repositorys.chapter

import android.util.Log
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.domain.repositorys.base.Resource
import io.silv.manga.domain.suspendRunCatching
import io.silv.manga.local.workers.handlers.AzukiHandler
import io.silv.manga.local.workers.handlers.BiliHandler
import io.silv.manga.local.workers.handlers.ComikeyHandler
import io.silv.manga.local.workers.handlers.MangaHotHandler
import io.silv.manga.local.workers.handlers.MangaPlusHandler
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.chapter.Chapter
import io.silv.manga.network.mangadex.requests.ChapterListRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

interface ChapterImageRepository {

    suspend fun getChapterImageUrls(chapterId: String, externalUrl: String): Result<List<String>>

    suspend fun getChapterImages(id: String): Flow<Resource<Pair<Chapter, List<String>>>>
}

class ChapterImageRepositoryImpl(
    private val mangaDexApi: MangaDexApi,
    private val azukiHandler: AzukiHandler,
    private val mangaHotHandler: MangaHotHandler,
    private val biliHandler: BiliHandler,
    private val comikeyHandler: ComikeyHandler,
    private val mangaPlusHandler: MangaPlusHandler
): ChapterImageRepository {


    private suspend fun getImages(url: String, chapterId: String) = suspendRunCatching {
        withContext(Dispatchers.IO) {
            Log.d("ChapterImageRepository", "matching externalUrl: $url")
            when {
                "mangaplus.shueisha" in url -> mangaPlusHandler.fetchImageUrls(url)
                    .also { Log.d("IMAGES", "$it") }
                "azuki.co" in url -> azukiHandler.fetchImageUrls(url)
                "mangahot.jp" in url -> mangaHotHandler.fetchImageUrls(url)
                "bilibilicomics.com" in url -> biliHandler.fetchImageUrls(url)
                "comikey.com" in url -> comikeyHandler.fetchImageUrls(url)
                url.isBlank() -> {
                    val response = mangaDexApi.getChapterImages(chapterId)
                        .getOrThrow()
                    response.chapter.data.map {
                        "${response.baseUrl}/data/${response.chapter.hash}/$it"
                    }
                }
                else -> error("not supported read on web")
            }
        }
    }

    override suspend fun getChapterImageUrls(chapterId: String, externalUrl: String): Result<List<String>> {
        return getImages(externalUrl, chapterId)
    }


    override suspend fun getChapterImages(id: String): Flow<Resource<Pair<Chapter,List<String>>>> = flow {
        emit(Resource.Loading)
        val chapter = mangaDexApi.getChapterData(
            ChapterListRequest(
                ids = listOf(id.ifEmpty { error("id was not found") })
            )
        )
            .getOrThrow()
            .data
            .first()
        val externalUrl = chapter.attributes.externalUrl?.replace("\\", "") ?: ""
        getImages(externalUrl, chapter.id)
            .onSuccess {
                emit(
                    Resource.Success(chapter to it)
                )
            }
            .onFailure {
                emit(
                    Resource.Failure(it.message ?: "error", null)
                )
            }
    }
}