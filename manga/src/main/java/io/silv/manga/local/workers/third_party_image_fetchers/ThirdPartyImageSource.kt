package io.silv.manga.local.workers.third_party_image_fetchers

import okhttp3.Headers

interface ThirdPartyImageSource {

    suspend fun fetchImageUrls(externalUrl: String): List<String>

    private val USER_AGENT
        get() = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:106.0) Gecko/20100101 Firefox/106.0"

    val headers
        get() = Headers.Builder()
            .add("User-Agent", USER_AGENT)
            .build()
}
