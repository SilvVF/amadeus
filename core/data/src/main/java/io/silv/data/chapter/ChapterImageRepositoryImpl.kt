package io.silv.data.chapter

import android.util.Log
import com.skydoves.sandwich.getOrThrow
import io.silv.common.coroutine.suspendRunCatching
import io.silv.common.model.Resource
import io.silv.network.image_sources.ImageSourceFactory
import io.silv.network.model.chapter.Chapter
import io.silv.network.requests.ChapterListRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

internal class ChapterImageRepositoryImpl(
    private val mangaDexApi: io.silv.network.MangaDexApi,
    private val imageSourceFactory: ImageSourceFactory,
): ChapterImageRepository {

    private suspend fun getImages(url: String, chapterId: String) = suspendRunCatching {
        withContext(Dispatchers.IO) {
            Log.d("ChapterImageRepository", "matching externalUrl: $url")
            when {
                url.isBlank() -> {
                    val response = mangaDexApi.getChapterImages(chapterId).getOrThrow()
                    response.chapter.data.map {
                        "${response.baseUrl}/data/${response.chapter.hash}/$it"
                    }
                }
                else -> imageSourceFactory.getSource(url).fetchImageUrls(url)
            }
        }
    }

    override suspend fun getChapterImageUrls(chapterId: String, externalUrl: String): Result<List<String>> {
        return getImages(externalUrl, chapterId)
    }

    override suspend fun getChapterImages(id: String): Flow<Resource<Pair<Chapter, List<String>>>> = flow {
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