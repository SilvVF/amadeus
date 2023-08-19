package io.silv.manga.local.workers.third_party_image_fetchers

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class MangaHotHandler(
    private val client: OkHttpClient
): ThirdPartyImageSource {

    private val baseUrl = "https://mangahot.jp"
    private val apiUrl = "https://api.mangahot.jp"

    private fun pageListParse(response: Response): List<String> {
        return Json.parseToJsonElement(response.body!!.string())
            .jsonObject["content"]!!.jsonObject["contentUrls"]!!
            .jsonArray.mapIndexed { index, element ->
                val url = element.jsonPrimitive.content
                url
            }
    }

    override suspend fun fetchImageUrls(externalUrl: String): List<String> {
        val url = externalUrl.substringBefore("?").replace(baseUrl, apiUrl)
            .replace("viewer", "v1/works/storyDetail")

        val request = Request.Builder()
            .headers(headers)
            .url(url)
            .build()

        return pageListParse(
            client.newCall(request).execute()
        )
    }
}