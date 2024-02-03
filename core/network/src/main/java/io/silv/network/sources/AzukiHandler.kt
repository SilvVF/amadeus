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

internal class AzukiHandler(
    private val client: HttpClient
): ImageSource() {

    private val apiUrl = "https://production.api.azuki.co"

    override suspend fun fetchImageUrls(externalUrl: String): List<String> {
        val chapterId =
            externalUrl
                .substringAfterLast("/")
                .substringBefore("?")

        return pageListParse(
            client.get {
                url("$apiUrl/chapter/$chapterId/pages/v0")
                headers(requestHeaders)
            }
        )
    }

    private suspend fun pageListParse(response: HttpResponse): List<String> {
        val urls =
            Json.parseToJsonElement(response.bodyAsText())
                .jsonObject["pages"]!!
                .jsonArray.mapIndexed { index, element ->
                    element.jsonObject["image_wm"]!!.jsonObject["webp"]!!.jsonArray[1].jsonObject["url"]!!.jsonPrimitive.content
                }
        return urls
    }
}
