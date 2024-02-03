package io.silv.network.sources

import io.ktor.http.HeadersBuilder

abstract class ImageSource {

    abstract suspend fun fetchImageUrls(externalUrl: String): List<String>

    internal open val requestHeaders: HeadersBuilder.() -> Unit = {
        append("User-Agent", USER_AGENT)
    }


    companion object {
        internal const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:106.0) Gecko/20100101 Firefox/106.0"
    }
}
