package io.silv.network.sources

import okhttp3.Headers

abstract class ImageSource {
    abstract suspend fun fetchImageUrls(externalUrl: String): List<String>

    internal val USER_AGENT
        get() = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:106.0) Gecko/20100101 Firefox/106.0"

    internal open val headers
        get() =
            Headers.Builder()
                .add("User-Agent", USER_AGENT)
                .build()
}
