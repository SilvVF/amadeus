package io.silv.network.sources

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.headers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.Jsoup

class ComikeyHandler(
    private val client: HttpClient,
) : ImageSource() {

    private val baseUrl = "https://comikey.com"
    private val apiUrl = "$baseUrl/sapi"

    override suspend fun fetchImageUrls(externalUrl: String): List<String> {
        val httpUrl = externalUrl.toHttpUrl()
        val mangaId = getMangaId(httpUrl.pathSegments[1])
        val response = client.pageListRequest(mangaId, httpUrl.pathSegments[2])

        return pageListParse(
            client.getActualPageList(response) ?: error("page not available for free")
        )
    }

    private suspend fun getMangaId(mangaUrl: String): Int {
        val response =
            client.get {
                url("$baseUrl/read/$mangaUrl")
                headers(requestHeaders)
            }

        val url =
            Jsoup.parse(response.bodyAsText(), response.request.url.toString())
                .selectFirst("meta[property=og:url]")!!.attr("content")
        return url.trimEnd('/').substringAfterLast('/').toInt()
    }

    private suspend fun HttpClient.pageListRequest(
        mangaId: Int,
        chapterGuid: String,
    ): HttpResponse {
        return get {
            url("$apiUrl/comics/$mangaId/read?format=json&content=EPI-$chapterGuid")
            headers(requestHeaders)
        }
    }

    private suspend fun HttpClient.getActualPageList(response: HttpResponse): HttpResponse? {
        val element = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val ok = element["ok"]?.jsonPrimitive?.booleanOrNull ?: false
        if (ok.not()) {
            return null
        }
        val url = element["href"]?.jsonPrimitive!!.content
        return get {
            url(url)
            headers(requestHeaders)
        }
    }

    private suspend fun pageListParse(response: HttpResponse): List<String> {
        return Json.parseToJsonElement(response.bodyAsText())
            .jsonObject["readingOrder"]!!
            .jsonArray.mapIndexed { index, element ->
                val url = element.jsonObject["href"]!!.jsonPrimitive.content
                url
            }
    }
}
