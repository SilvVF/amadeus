package io.silv.domain.chapter.model

import androidx.compose.runtime.Stable
import io.silv.common.model.ChapterResource
import io.silv.common.model.ProgressState
import io.silv.common.time.localDateTimeNow
import io.silv.common.time.minus
import io.silv.domain.DateTimeAsLongSerializer
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import java.util.UUID

@Stable
data class Chapter(
    val id: String,
    val url: String,
    val bookmarked: Boolean,
    val downloaded: Boolean,
    val mangaId: String,
    val title: String,
    val volume: Int,
    val chapter: Double,
    val pages: Int = 0,
    val lastReadPage: Int? = null,
    val translatedLanguage: String,
    val uploader: String,
    val scanlationGroupToId: Pair<String, String>? = null,
    val userToId: Pair<String, String>? = null,
    val version: Int,
    @Serializable(with = DateTimeAsLongSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = DateTimeAsLongSerializer::class)
    val updatedAt: LocalDateTime,
    @Serializable(with = DateTimeAsLongSerializer::class)
    val readableAt: LocalDateTime,
    val ableToDownload: Boolean,
) {
    val scanlator = scanlationGroupToId?.first ?: ""
    val scanlatorOrNull = scanlationGroupToId?.first
    val scanlatorid = scanlationGroupToId?.second ?: ""

    val progress: ProgressState
        get() = when(lastReadPage) {
            null -> ProgressState.NotStarted
            pages -> ProgressState.Finished
            else -> ProgressState.Reading
        }

    private val daysSinceCreated: Long
        get() = (localDateTimeNow() - this.createdAt).inWholeDays

    val validNumber: Boolean
        get() = this.chapter >= 0.0

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

    companion object {
        fun stub(mangaId: String, volume: Int = 1, chapter: Double = 1.0): Chapter {
            return Chapter(
                id = UUID.randomUUID().toString(),
                url = "",
                bookmarked = false,
                downloaded = false,
                mangaId = mangaId,
                title = "Random title",
                volume = volume,
                chapter = chapter,
                pages = (4..40).random(),
                lastReadPage = 0,
                translatedLanguage = "en",
                uploader = UUID.randomUUID().toString(),
                scanlationGroupToId = "" to "",
                userToId = null,
                version = 1,
                createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                updatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                readableAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                ableToDownload = false
            )
        }
    }
}

fun Chapter.toResource(): ChapterResource {
    val c = this
    return object : ChapterResource {
        override val id: String
            get() = c.id
        override val mangaId: String
            get() = c.mangaId
        override val volume: Int
            get() = c.volume
        override val chapter: Double
            get() = c.chapter
        override val scanlator: String
            get() = c.scanlator
        override val title: String
            get() = c.title
        override val url: String
            get() = c.url
    }
}
