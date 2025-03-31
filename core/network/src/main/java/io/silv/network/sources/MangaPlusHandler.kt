package io.silv.network.sources

import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.Sender
import io.ktor.client.plugins.observer.wrapWithContent
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HeadersBuilder
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.toByteArray
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

class MangaPlusHandler: ImageSource() {
    private val baseUrl = "https://jumpg-webapi.tokyo-cdn.com/api"

    private val client = HttpClient(OkHttp)

    override val requestHeaders: HeadersBuilder.() -> Unit = {
        append("Origin", WEB_URL)
        append("Referer", WEB_URL)
        append("User-Agent", USER_AGENT)
        append("SESSION-TOKEN", UUID.randomUUID().toString())
    }

    override suspend fun fetchImageUrls(externalUrl: String): List<String> {
        val httpSend = client.plugin(HttpSend)

        httpSend.intercept { request -> imageIntercept(request) }

        val chapterId = externalUrl.substringAfterLast("/")

        val response = client.get {
            url("$baseUrl/manga_viewer?chapter_id=$chapterId&split=yes&img_quality=high&format=json")
            headers(requestHeaders)
        }

        return pageListParse(response)
    }


    private suspend fun pageListParse(response: HttpResponse): List<String> {
        val r = response.bodyAsChannel()

        r.awaitContent()

        val json =  Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = true
        }

        val result = json.decodeFromString<MangaPlusResponse>(r.toByteArray().decodeToString())

        checkNotNull(result.success) {
            result.error!!.popups
                .firstOrNull { it.language == Language.ENGLISH } ?: "Error with MangaPlus"
        }

        return result.success.mangaViewer!!.pages
            .mapNotNull { it.mangaPage }
            .mapIndexed { i, page ->
                val encryptionKey =
                    if (page.encryptionKey == null) "" else "&encryptionKey=${page.encryptionKey}"
                "${page.imageUrl}$encryptionKey"
            }
    }

    private suspend fun Sender.imageIntercept(request: HttpRequestBuilder): HttpClientCall {

        if (!request.url.parameters.contains("encryptionKey")) {
            return execute(request)
        }

        val response = execute(request)

        val encryptionKey = response.request.url.parameters["encryptionKey"]!!
        val image = decodeImage(encryptionKey, response.body())
        // Change the url and remove the encryptionKey to avoid detection.
        request.url.parameters.remove("encryptionKey")

        return response.wrapWithContent(ByteReadChannel(image))
    }

    private fun decodeImage(
        encryptionKey: String,
        image: ByteArray,
    ): ByteArray {
        val keyStream =
            HEX_GROUP
                .findAll(encryptionKey)
                .map { it.groupValues[1].toInt(16) }
                .toList()

        val content =
            image
                .map { it.toInt() }
                .toMutableList()

        val blockSizeInBytes = keyStream.size

        for ((i, value) in content.iterator().withIndex()) {
            content[i] = value xor keyStream[i % blockSizeInBytes]
        }

        return ByteArray(content.size) { pos -> content[pos].toByte() }
    }

    companion object {
        private const val WEB_URL = "https://mangaplus.shueisha.co.jp"
        private val HEX_GROUP = "(.{1,2})".toRegex()
    }
}

@Serializable
data class MangaPlusResponse(
    val success: MangaPLusSuccessResult? = null,
    val error: MangaPlusErrorResult? = null,
)

@Serializable
data class MangaPlusErrorResult(
    val popups: List<Popup> = emptyList(),
)

@Serializable
data class Popup(
    val subject: String,
    val body: String,
    val language: Language? = Language.ENGLISH,
)

@Serializable
data class MangaPLusSuccessResult(
    val isFeaturedUpdated: Boolean? = false,
    val titleRankingView: TitleRankingView? = null,
    val titleDetailView: TitleDetailView? = null,
    val mangaViewer: MangaViewer? = null,
    val allTitlesViewV2: AllTitlesViewV2? = null,
    val webHomeViewV3: WebHomeViewV3? = null,
)

@Serializable
data class TitleRankingView(val titles: List<Title> = emptyList())

@Serializable
data class AllTitlesViewV2(
    @SerialName("AllTitlesGroup") val allTitlesGroup: List<AllTitlesGroup> = emptyList(),
)

@Serializable
data class AllTitlesGroup(
    val theTitle: String,
    val titles: List<Title> = emptyList(),
)

@Serializable
data class WebHomeViewV3(val groups: List<UpdatedTitleV2Group> = emptyList())

@Serializable
data class TitleDetailView(
    val title: Title,
    val titleImageUrl: String,
    val overview: String,
    val backgroundImageUrl: String,
    val nextTimeStamp: Int = 0,
    val viewingPeriodDescription: String = "",
    val nonAppearanceInfo: String = "",
    val firstChapterList: List<Chapter> = emptyList(),
    val lastChapterList: List<Chapter> = emptyList(),
    val isSimulReleased: Boolean = false,
    val chaptersDescending: Boolean = true,
)

@Serializable
data class MangaViewer(val pages: List<MangaPlusPage> = emptyList())

@Serializable
data class Title(
    val titleId: Int,
    val name: String,
    val author: String,
    val portraitImageUrl: String,
    val landscapeImageUrl: String,
    val viewCount: Int = 0,
    val language: Language? = Language.ENGLISH,
)

enum class Language {
    ENGLISH,
    SPANISH,
    FRENCH,
    INDONESIAN,
    PORTUGUESE_BR,
    RUSSIAN,
    THAI,
}

@Serializable
data class UpdatedTitleV2Group(
    val groupName: String,
    val titleGroups: List<OriginalTitleGroup> = emptyList(),
)

@Serializable
data class OriginalTitleGroup(
    val theTitle: String,
    val titles: List<UpdatedTitle> = emptyList(),
)

@Serializable
data class UpdatedTitle(val title: Title)

@Serializable
data class Chapter(
    val titleId: Int,
    val chapterId: Int,
    val name: String,
    val subTitle: String? = null,
    val startTimeStamp: Int,
    val endTimeStamp: Int,
    val isVerticalOnly: Boolean = false,
)

@Serializable
data class MangaPlusPage(val mangaPage: MangaPage? = null)

@Serializable
data class MangaPage(
    val imageUrl: String,
    val width: Int,
    val height: Int,
    val encryptionKey: String? = null,
)
