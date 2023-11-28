package io.silv.network.image_sources

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup

class ComikeyHandler(
    private val client: OkHttpClient
): ImageSource() {

    private val baseUrl = "https://comikey.com"
    private val apiUrl = "$baseUrl/sapi"

    override suspend fun fetchImageUrls(externalUrl: String): List<String> {
        val httpUrl = externalUrl.toHttpUrl()
        val mangaId = getMangaId(httpUrl.pathSegments[1])
        val response = client.newCall(
            pageListRequest(mangaId, httpUrl.pathSegments[2])
        )
            .execute()
        val request =
            getActualPageList(response) ?: error("page not available for free")
        return pageListParse(client.newCall(request).execute())
    }

    private fun getMangaId(mangaUrl: String): Int {
        val response = client.newCall(
            Request.Builder()
                .url("$baseUrl/read/$mangaUrl")
                .headers(headers)
                .build()
        )
            .execute()

        val url = Jsoup.parse(response.body?.string()!!, response.request.url.toString())
            .selectFirst("meta[property=og:url]")!!.attr("content")
        return url.trimEnd('/').substringAfterLast('/').toInt()
    }

    private fun pageListRequest(mangaId: Int, chapterGuid: String): Request {
        return Request.Builder()
            .url("$apiUrl/comics/$mangaId/read?format=json&content=EPI-$chapterGuid")
            .headers(headers)
            .build()
    }

    private fun getActualPageList(response: Response): Request? {
        val element = Json.parseToJsonElement(response.body!!.string()).jsonObject
        val ok = element["ok"]?.jsonPrimitive?.booleanOrNull ?: false
        if (ok.not()) {
            return null
        }
        val url = element["href"]?.jsonPrimitive!!.content
        return Request.Builder()
            .url(url)
            .headers(headers)
            .build()
    }

    private fun pageListParse(response: Response): List<String> {
        return Json.parseToJsonElement(response.body!!.string())
            .jsonObject["readingOrder"]!!
            .jsonArray.mapIndexed { index, element ->
                val url = element.jsonObject["href"]!!.jsonPrimitive.content
                url
            }
    }
}