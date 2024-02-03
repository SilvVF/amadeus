package io.silv.network.sources

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

class ImageSourceFactory(
    private val client: HttpClient,
    private val json: Json,
) {
    fun getSource(url: String): ImageSource {
        return when {
            "mangaplus.shueisha" in url -> MangaPlusHandler()
            "azuki.co" in url -> AzukiHandler(client)
            "mangahot.jp" in url -> MangaHotHandler(client)
            "bilibilicomics.com" in url -> BiliHandler(client)
            "comikey.com" in url -> ComikeyHandler(client)
            else -> error("not supported read on web")
        }
    }
}
