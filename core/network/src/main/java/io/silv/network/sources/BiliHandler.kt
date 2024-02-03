package io.silv.network.sources

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HeadersBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class BiliHandler(
    private val client: HttpClient
) : ImageSource() {
    private val baseUrl = "https://www.bilibilicomics.com"

    override suspend fun fetchImageUrls(externalUrl: String): List<String> {
        return fetchPageList(externalUrl)
    }

    override val requestHeaders: HeadersBuilder.() -> Unit = {
        append("Accept", ACCEPT_JSON)
        append("Origin", baseUrl)
        append("Referer", "$baseUrl/")
    }

    private fun getChapterUrl(externalUrl: String): String {
        val comicId =
            externalUrl.substringAfterLast("/mc")
                .substringBefore('/')
                .toInt()
        val episodeId =
            externalUrl.substringAfterLast('/')
                .substringBefore('?')
                .toInt()
        return "/mc$comicId/$episodeId"
    }

    private suspend fun fetchPageList(chapterUrl: String): List<String> {
        val response = client.pageListRequest(getChapterUrl(chapterUrl))
        return pageListParse(response)
    }

    private suspend fun HttpClient.pageListRequest(chapterUrl: String): HttpResponse {
        val chapterId = chapterUrl.substringAfterLast("/").toInt()

        val jsonPayload = buildJsonObject { put("ep_id", chapterId) }
        val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders: HeadersBuilder.() -> Unit = {
            set("Referer", baseUrl + chapterUrl)
        }

        return post {
            url("$baseUrl/$BASE_API_ENDPOINT/GetImageIndex?device=pc&platform=web")
            headers(requestHeaders)
            headers(newHeaders)
            setBody(requestBody)
        }
    }

    private suspend fun pageListParse(response: HttpResponse): List<String> {
        val result = response.body<BilibiliResultDto<BilibiliReader>>()
        if (result.message.contains("need buy episode")) {
            throw Exception(
                "Chapter is unavailable, requires reading and/or purchasing on BililBili"
            )
        }
        if (result.code != 0) {
            return emptyList()
        }
        val baseUrls = result.data!!.images.map { it.path }

        val imageResponse = client.imageUrlRequest(baseUrls)

        return imageUrlParse(imageResponse)
    }


    private suspend fun imageUrlParse(response: HttpResponse): List<String> {
        val result = response.body<BilibiliResultDto<List<BilibiliPageDto>>>()
        return result.data!!.mapIndexed { index, page ->
            "${page.url}?token=${page.token}"
        }
    }

    private suspend fun HttpClient.imageUrlRequest(baseUrls: List<String>): HttpResponse {
        val jsonPayload =
            buildJsonObject {
                put(
                    "urls",
                    buildJsonArray {
                        baseUrls.forEach { add(it) }
                    }.toString(),
                )
            }
        val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders: HeadersBuilder.() -> Unit = {
            append("Content-Length", requestBody.contentLength().toString())
            append("Content-Type", requestBody.contentType().toString())
        }

        return post {
            url("$baseUrl/$BASE_API_ENDPOINT/ImageToken?device=pc&platform=web")
            headers(requestHeaders)
            headers(newHeaders)
            setBody(requestBody)
        }
    }

    @Serializable
    data class BilibiliPageDto(
        val token: String,
        val url: String,
    )

    @Serializable
    data class BilibiliResultDto<T>(
        val code: Int = 0,
        val data: T? = null,
        @SerialName("msg") val message: String = "",
    )

    @Serializable
    data class BilibiliReader(
        val images: List<BilibiliImageDto> = emptyList(),
    )

    @Serializable
    data class BilibiliImageDto(
        val path: String,
    )

    @Serializable
    data class BilibiliComicDto(
        @SerialName("author_name") val authorName: List<String> = emptyList(),
        @SerialName("classic_lines") val classicLines: String = "",
        @SerialName("comic_id") val comicId: Int = 0,
        @SerialName("ep_list") val episodeList: List<BilibiliEpisodeDto> = emptyList(),
        val id: Int = 0,
        @SerialName("is_finish") val isFinish: Int = 0,
        @SerialName("season_id") val seasonId: Int = 0,
        val styles: List<String> = emptyList(),
        val title: String,
        @SerialName("vertical_cover") val verticalCover: String = "",
    )

    @Serializable
    data class BilibiliEpisodeDto(
        val id: Int,
        @SerialName("is_locked") val isLocked: Boolean,
        @SerialName("ord") val order: Float,
        @SerialName("pub_time") val publicationTime: String,
        val title: String,
    )

    companion object {
        private const val BASE_API_ENDPOINT = "twirp/comic.v1.Comic"
        private const val ACCEPT_JSON = "application/json, text/plain, */*"
        private val JSON_MEDIA_TYPE = "application/json;charset=UTF-8".toMediaType()
    }
}
