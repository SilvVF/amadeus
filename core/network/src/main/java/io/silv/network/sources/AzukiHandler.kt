package io.silv.network.sources

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

internal class AzukiHandler(
    private val client: OkHttpClient,
) : ImageSource() {
    private val apiUrl = "https://production.api.azuki.co"

    override suspend fun fetchImageUrls(externalUrl: String): List<String> {
        val chapterId =
            externalUrl
                .substringAfterLast("/")
                .substringBefore("?")
        val request =
            Request.Builder()
                .url("$apiUrl/chapter/$chapterId/pages/v0")
                .also { Log.d("Azuki", it.toString()) }
                .headers(headers)
                .build()
        return pageListParse(
            client.newCall(request).execute(),
        )
    }

    private fun pageListParse(response: Response): List<String> {
        val urls =
            Json.parseToJsonElement(response.body!!.string().also { Log.d("Azuki", it) })
                .jsonObject["pages"]!!
                .jsonArray.mapIndexed { index, element ->
                    element.jsonObject["image_wm"]!!.jsonObject["webp"]!!.jsonArray[1].jsonObject["url"]!!.jsonPrimitive.content
                }
        return urls
            .also { Log.d("Azuki", it.toString()) }
    }
}
