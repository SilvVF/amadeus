package io.silv.manga.local.workers.handlers

import okhttp3.OkHttpClient
import okhttp3.Request

class MangaPlusHandler(
    private val client: OkHttpClient
): ThirdPartyImageSource {

    override suspend fun fetchImageUrls(
        externalUrl: String
    ): List<String> {

        val mangaPlusApiId = externalUrl.takeLastWhile { c -> c != '/' }

        val request = Request.Builder()
            .headers(headers)
            .url("https://jumpg-webapi.tokyo-cdn.com/api/manga_viewer?chapter_id=$mangaPlusApiId&split=yes&img_quality=high")
            .build()

        val response = client.newCall(request).execute()
        val stringBody = response.body?.string() ?: ""

        return buildList {
            for (substring in  stringBody.split("https://mangaplus.shueisha.co.jp/drm/title/")) {
                if (substring.contains("chapter_thumbnail")) { continue }
                val endIdx = substring.indexOf("&duration=").takeIf { it != -1 } ?: continue
                val duration = substring.substring(endIdx + "&duration=".length, substring.length)
                    .takeWhile { c -> c.isDigit() }
                add(
                    "https://mangaplus.shueisha.co.jp/drm/title/" +
                            substring.substring(0, endIdx) + "&duration=" +
                            duration.runCatching { duration.take(5) }.getOrDefault(duration)
                )
            }
        }
    }
}