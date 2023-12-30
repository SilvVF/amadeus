package io.silv.network.sources

import com.skydoves.sandwich.getOrThrow
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HeadersBuilder
import io.silv.common.model.ChapterResource
import io.silv.common.model.Page
import io.silv.network.MangaDexApi
import kotlinx.serialization.Serializable
import okhttp3.CacheControl
import java.util.Date
import kotlin.time.Duration.Companion.minutes

@Serializable
private data class AtHomeDto(
    val baseUrl: String,
    val chapter: AtHomeChapterDto,
)

@Serializable
private data class AtHomeChapterDto(
    val hash: String,
    val data: List<String>,
    val dataSaver: List<String>,
)

@Serializable
private data class ImageReportDto(
    val url: String,
    val success: Boolean,
    val bytes: Int?,
    val cached: Boolean,
    val duration: Long,
)


class HttpSource(
    private val mangaDexApi: MangaDexApi,
    private val client: HttpClient,
) {
    private val tokenTracker = mutableMapOf<String, Long>()

    fun getImageUrl(page: Page): String {
        return page.imageUrl ?: ""
    }

    /**
     * create an md at home Request
     */
    private fun mdAtHomeRequest(
        tokenRequestUrl: String,
        headers: HeadersBuilder,
        cacheControl: CacheControl.Builder,
    ): HttpRequestBuilder {
        val cache = cacheControl.build()
        if (cache.noCache) {
            tokenTracker[tokenRequestUrl] = Date().time
        }
        return HttpRequestBuilder().apply {
            url(tokenRequestUrl)
            headers {
                headers.build()
            }
            cacheControl.build()
        }
    }
    /**
     * Get the MD@Home URL.
     */
    private suspend fun getMdAtHomeUrl(
        tokenRequestUrl: String,
        client: HttpClient,
        headers: HeadersBuilder,
        cacheControl: CacheControl.Builder,
    ): String {
        val request = mdAtHomeRequest(tokenRequestUrl, headers, cacheControl)
        val response = client.get(request)

        // This check is for the error that causes pages to fail to load.
        // It should never be entered, but in case it is, we retry the request.
        if (response.status.value == 504) {
            return getMdAtHomeUrl(
                tokenRequestUrl,
                client,
                headers,
                CacheControl.Builder().noCache()
            )
        }

        return response.body<AtHomeDto>().baseUrl
    }

    /**
     * Check the token map to see if the MD@Home host is still valid.
     */
    private suspend fun getValidImageUrlForPage(page: Page, headers: HeadersBuilder): HttpRequestBuilder.() -> Unit {
        val (host, tokenRequestUrl, time) = page.url.split(",")

        val mdAtHomeServerUrl =
            when (Date().time - time.toLong() > 5.minutes.inWholeMilliseconds) {
                false -> host
                true -> {
                    val tokenLifespan = Date().time - (tokenTracker[tokenRequestUrl] ?: 0)
                    val cacheControl = if (tokenLifespan > 5.minutes.inWholeMilliseconds) {
                        CacheControl.Builder().noCache()
                    } else {
                        CacheControl.Builder().onlyIfCached()
                    }
                    getMdAtHomeUrl(tokenRequestUrl, client, headers, cacheControl)
                }
            }
        return {
            url(page.imageUrl!!.replaceBefore("/data", mdAtHomeServerUrl))
            headers.build()
        }
    }

    private val headers =  HeadersBuilder().apply {
        set("Referer", "${mangaDexApi.mangaDexUrl}/")
    }


    suspend fun getImage(page: Page): HttpResponse {
        return client.get(getValidImageUrlForPage(page, headers))

    }

    suspend fun getPageList(chapter: ChapterResource): List<Page> {
        val response = mangaDexApi.getChapterImages(chapter.id).getOrThrow()
        val host = response.baseUrl
        val atHomeRequestUrl = "https://api.mangadex.org/at-home/server/${chapter.id}"

        // Have to add the time, and url to the page because pages timeout within 30 minutes now.
        val now = Date().time

        return response.chapter.data.mapIndexed { index, data ->
            Page(
                index = index,
                url = "$host,$atHomeRequestUrl,$now",
                imageUrl = "${response.baseUrl}/data/${response.chapter.hash}/$data",
            )
        }
    }
}