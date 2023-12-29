package io.silv.reader.loader

import com.skydoves.sandwich.getOrThrow
import io.silv.common.model.ChapterResource
import io.silv.common.model.Page
import io.silv.data.download.await
import io.silv.network.MangaDexApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.Date
import kotlin.time.Duration.Companion.minutes

@Serializable
data class AtHomeDto(
    val baseUrl: String,
    val chapter: AtHomeChapterDto,
)

@Serializable
data class AtHomeChapterDto(
    val hash: String,
    val data: List<String>,
    val dataSaver: List<String>,
)

@Serializable
data class ImageReportDto(
    val url: String,
    val success: Boolean,
    val bytes: Int?,
    val cached: Boolean,
    val duration: Long,
)


class HttpSource(
    private val mangaDexApi: MangaDexApi,
    private val client: OkHttpClient,
    private val json: Json,
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
        headers: Headers,
        cacheControl: CacheControl,
    ): Request {
        if (cacheControl == CacheControl.FORCE_NETWORK) {
            tokenTracker[tokenRequestUrl] = Date().time
        }
        return Request.Builder()
            .url(tokenRequestUrl)
            .headers(headers)
            .cacheControl(cacheControl)
            .build()
    }
    /**
     * Get the MD@Home URL.
     */
    private fun getMdAtHomeUrl(
        tokenRequestUrl: String,
        client: OkHttpClient,
        headers: Headers,
        cacheControl: CacheControl,
    ): String {
        val request = mdAtHomeRequest(tokenRequestUrl, headers, cacheControl)
        val response = client.newCall(request).execute()

        // This check is for the error that causes pages to fail to load.
        // It should never be entered, but in case it is, we retry the request.
        if (response.code == 504) {
            return getMdAtHomeUrl(tokenRequestUrl, client, headers, CacheControl.FORCE_NETWORK)
        }

        return response.use { json.decodeFromString<AtHomeDto>(it.body!!.string()).baseUrl }
    }

    /**
     * Check the token map to see if the MD@Home host is still valid.
     */
    private fun getValidImageUrlForPage(page: Page, headers: Headers): Request {
        val (host, tokenRequestUrl, time) = page.url.split(",")

        val mdAtHomeServerUrl =
            when (Date().time - time.toLong() > 5.minutes.inWholeMilliseconds) {
                false -> host
                true -> {
                    val tokenLifespan = Date().time - (tokenTracker[tokenRequestUrl] ?: 0)
                    val cacheControl = if (tokenLifespan > 5.minutes.inWholeMilliseconds) {
                        CacheControl.FORCE_NETWORK
                    } else {
                        CacheControl.FORCE_CACHE
                    }
                    getMdAtHomeUrl(tokenRequestUrl, client, headers, cacheControl)
                }
            }
        return Request.Builder().url(
            page.imageUrl!!.replaceBefore("/data", mdAtHomeServerUrl)
        ).headers(headers).build()
    }

    private val headers = Headers.Builder().apply {
        set("Referer", "${mangaDexApi.mangaDexUrl}/")
    }
        .build()

    suspend fun getImage(page: Page): Response {
        return client.newCall(
            getValidImageUrlForPage(page, headers)
        )
            .await()
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
