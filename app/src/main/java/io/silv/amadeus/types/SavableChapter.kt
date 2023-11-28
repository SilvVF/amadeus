package io.silv.amadeus.types

import android.os.Parcel
import android.os.Parcelable
import io.silv.common.model.ProgressState
import io.silv.common.time.localDateTimeNow
import io.silv.common.time.minus
import io.silv.database.entity.chapter.ChapterEntity
import kotlinx.datetime.LocalDateTime
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.json.Json

private val implementedImageSources = listOf(
    "mangaplus.shueisha",
    "azuki.co",
    "mangahot.jp",
    "bilibilicomics.com",
    "comikey.com"
)

@Parcelize
@kotlinx.serialization.Serializable
data class SavableChapter(
    val id: String,
    val bookmarked: Boolean,
    val downloaded: Boolean,
    val progress: ProgressState,
    val mangaId: String,
    val imageUris: List<String> = emptyList(),
    val title: String,
    val volume: Int,
    val chapter: Long,
    val pages: Int = 0,
    val lastReadPage: Int,
    val translatedLanguage: String,
    val uploader: String,
    val externalUrl: String? = null,
    val scanlationGroupToId: Pair<String, String>? = null,
    val userToId: Pair<String,String>? = null,
    val version: Int,
    @kotlinx.serialization.Serializable(with = DateTimeAsLongSerializer::class)
    val createdAt: LocalDateTime,
    @kotlinx.serialization.Serializable(with = DateTimeAsLongSerializer::class)
    val updatedAt: LocalDateTime,
    @kotlinx.serialization.Serializable(with = DateTimeAsLongSerializer::class)
    val readableAt: LocalDateTime,
    val ableToDownload: Boolean,
): Parcelable {

    constructor(entity: ChapterEntity): this(
        id = entity.id,
        bookmarked = entity.bookmarked,
        downloaded = entity.chapterImages.isNotEmpty(),
        progress = entity.progressState,
        mangaId = entity.mangaId,
        imageUris = entity.chapterImages,
        title = entity.title,
        volume = entity.volume,
        chapter = entity.chapterNumber,
        pages = entity.pages,
        translatedLanguage = entity.languageCode,
        uploader = entity.uploader ?: "",
        externalUrl = entity.externalUrl,
        lastReadPage = entity.lastPageRead,
        version = entity.version,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        readableAt = entity.readableAt,
        scanlationGroupToId = entity
            .scanlationGroup?.let { group ->
                entity.scanlationGroupId?.let { id ->
                    group to id
                }
            },
        userToId = entity
            .user?.let { user ->
                entity.userId?.let {id ->
                    user to id
                }
        },
        ableToDownload = entity.externalUrl == null || implementedImageSources.any { it in (entity.externalUrl ?: "") },
    )
    private val daysSinceCreated: Long
        get() = (localDateTimeNow() - this.createdAt).inWholeDays

    val validNumber: Boolean
        get() = this.chapter >= 0

    @IgnoredOnParcel
    val daysSinceCreatedString: String by lazy {
        daysSinceCreated.run {
            return@run if (this >= 365) {
                val yearsAgo = this / 365
                if (yearsAgo <= 1.0) {
                    "last year"
                } else {
                    "${this / 365} years ago"
                }
            } else {
                if (this <= 0.9) {
                    "today"
                } else {
                    "$this days ago"
                }
            }
        }
    }

    val read: Boolean = progress == ProgressState.Finished

    val started: Boolean = progress == ProgressState.Reading

    companion object : kotlinx.parcelize.Parceler<SavableChapter> {
        override fun SavableChapter.write(parcel: Parcel, flags: Int) {
            parcel.writeString(
                Json.encodeToString(
                    serializer(),
                    this
                )
            )
        }

        override fun create(parcel: Parcel): SavableChapter {
            return Json.decodeFromString(serializer(), parcel.readString() ?: "")
        }
    }
}
