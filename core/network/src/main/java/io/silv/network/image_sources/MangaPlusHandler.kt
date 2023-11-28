package io.silv.network.image_sources

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.UUID

class MangaPlusHandler(
    client: OkHttpClient,
    private val json: Json
): ImageSource() {

    private val baseUrl = "https://jumpg-webapi.tokyo-cdn.com/api"

    private val client = client.newBuilder()
        .addInterceptor { imageIntercept(it) }
        .build()

    override val headers = Headers.Builder()
        .add("Origin", WEB_URL)
        .add("Referer", WEB_URL)
        .add("User-Agent", USER_AGENT)
        .add("SESSION-TOKEN", UUID.randomUUID().toString()).build()


//    override suspend fun fetchImageUrls(
//        externalUrl: String
//    ): List<String> {
//
//        val mangaPlusApiId = externalUrl.takeLastWhile { c -> c != '/' }
//
//        val request = Request.Builder()
//            .headers(headers)
//            .url("https://jumpg-webapi.tokyo-cdn.com/api/manga_viewer?chapter_id=$mangaPlusApiId&split=yes&img_quality=high")
//            .build()
//
//        val response = client.newCall(request).execute()
//        val stringBody = response.body?.string() ?: ""
//
//        return buildList {
//            for (substring in  stringBody.split("https://mangaplus.shueisha.co.jp/drm/title/")) {
//                if (substring.contains("chapter_thumbnail")) { continue }
//                val endIdx = substring.indexOf("&duration=").takeIf { it != -1 } ?: continue
//                val duration = substring.substring(endIdx + "&duration=".length, substring.length)
//                    .takeWhile { c -> c.isDigit() }
//                add(
//                    "https://mangaplus.shueisha.co.jp/drm/title/" +
//                            substring.substring(0, endIdx) + "&duration=" +
//                            duration.runCatching { duration.take(5) }.getOrDefault(duration)
//                )
//            }
//        }
//    }

    override suspend fun fetchImageUrls(externalUrl: String): List<String> {
        val response = client.newCall(
            pageListRequest(externalUrl.substringAfterLast("/"))
        ).execute()
        return pageListParse(response)
    }

    private fun pageListRequest(chapterId: String): Request {
        return Request.Builder()
            .url("$baseUrl/manga_viewer?chapter_id=$chapterId&split=yes&img_quality=high&format=json")
            .headers(headers)
            .build()
    }

    private fun Response.asMangaPlusResponse(): MangaPlusResponse = use {
        json.decodeFromString(body!!.string())
    }

    private fun pageListParse(response: Response): List<String> {
        val result = response.asMangaPlusResponse()

        checkNotNull(result.success) { result.error!!.popups.firstOrNull { it.language == Language.ENGLISH } ?: "Error with MangaPlus" }

        return result.success.mangaViewer!!.pages
            .mapNotNull { it.mangaPage }
            .mapIndexed { i, page ->
                val encryptionKey =
                    if (page.encryptionKey == null) "" else "&encryptionKey=${page.encryptionKey}"
                "${page.imageUrl}$encryptionKey"
            }
    }

    private fun imageIntercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        if (!request.url.queryParameterNames.contains("encryptionKey")) {
            return chain.proceed(request)
        }

        val encryptionKey = request.url.queryParameter("encryptionKey")!!

        // Change the url and remove the encryptionKey to avoid detection.
        val newUrl = request.url.newBuilder().removeAllQueryParameters("encryptionKey").build()
        request = request.newBuilder().url(newUrl).build()

        val response = chain.proceed(request)

        val image = decodeImage(encryptionKey, response.body!!.bytes())

        val body = image.toResponseBody("image/jpeg".toMediaTypeOrNull())
        return response.newBuilder().body(body).build()
    }

    private fun decodeImage(encryptionKey: String, image: ByteArray): ByteArray {
        val keyStream = HEX_GROUP
            .findAll(encryptionKey)
            .map { it.groupValues[1].toInt(16) }
            .toList()

        val content = image
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