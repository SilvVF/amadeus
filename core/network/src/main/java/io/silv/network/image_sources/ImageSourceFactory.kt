package io.silv.network.image_sources

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient


class ImageSourceFactory(
    private val client: OkHttpClient,
    private val json: Json
) {

    fun getSource(url: String): ImageSource {
        return when {
            "mangaplus.shueisha" in url -> MangaPlusHandler(client, json)
            "azuki.co" in url -> AzukiHandler(client)
            "mangahot.jp" in url -> MangaHotHandler(client)
            "bilibilicomics.com" in url -> BiliHandler(client, json)
            "comikey.com" in url -> ComikeyHandler(client)
            else -> error("not supported read on web")
        }
    }
}