package io.silv.network.sources

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal class MangaHotHandler(
    private val client: HttpClient,
) : ImageSource() {
    private val baseUrl = "https://mangahot.jp"
    private val apiUrl = "https://api.mangahot.jp"

    private suspend fun pageListParse(response: HttpResponse): List<String> {
        return Json.parseToJsonElement(response.bodyAsText())
            .jsonObject["content"]!!.jsonObject["contentUrls"]!!
            .jsonArray.mapIndexed { index, element ->
                val url = element.jsonPrimitive.content
                url
            }
    }

    override suspend fun fetchImageUrls(externalUrl: String): List<String> {
        val url =
            externalUrl.substringBefore("?").replace(baseUrl, apiUrl)
                .replace("viewer", "v1/works/storyDetail")

        return pageListParse(
            client.get {
                url(url)
                headers(requestHeaders)
            }
        )
    }
}
