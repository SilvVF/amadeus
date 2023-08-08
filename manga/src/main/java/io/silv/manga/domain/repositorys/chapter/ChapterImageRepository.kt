package io.silv.manga.domain.repositorys.chapter

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface ChapterImageRepository {

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


    private suspend fun getImages(id: String) = suspendRunCatching {

        val chapter = mangaDexApi.getChapterData(
            ChapterListRequest(
                ids = listOf(id.ifEmpty { error("id was not found") })
            )
        )
            .getOrThrow()
            .data
            .first()

        val externalUrl = chapter.attributes.externalUrl?.replace("\\", "") ?: ""
        chapter to when {
            "mangaplus.shueisha" in externalUrl -> mangaPlusHandler.fetchImageUrls(externalUrl)
            "azuki.co" in externalUrl -> azukiHandler.fetchImageUrls(externalUrl)
            "mangahot.jp" in externalUrl -> mangaHotHandler.fetchImageUrls(externalUrl)
            "bilibilicomics.com" in externalUrl -> biliHandler.fetchImageUrls(externalUrl)
            "comikey.com" in externalUrl -> comikeyHandler.fetchImageUrls(externalUrl)
            externalUrl.isBlank() -> {
                val response = mangaDexApi.getChapterImages(chapter.id)
                    .getOrThrow()
                response.chapter.data.map {
                    "${response.baseUrl}/data/${response.chapter.hash}/$it"
                }
            }
            else -> error("not supported read on web")
        }
    }


    override suspend fun getChapterImages(id: String): Flow<Resource<Pair<Chapter,List<String>>>> = flow {
        emit(Resource.Loading)
        getImages(id)
            .onSuccess {
                emit(Resource.Success(it))
            }
            .onFailure {
                emit(
                    Resource.Failure(it.message ?: "error", null)
                )
            }
    }
}