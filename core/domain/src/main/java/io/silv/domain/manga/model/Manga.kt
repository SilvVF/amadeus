package io.silv.domain.manga.model

import androidx.compose.runtime.Stable
import io.silv.common.model.ContentRating
import io.silv.common.model.MangaResource
import io.silv.common.model.ProgressState
import io.silv.common.model.PublicationDemographic
import io.silv.common.model.ReadingStatus
import io.silv.common.model.Status
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.UUID

@Stable
data class Manga(
    val id: String,
    val inLibrary: Boolean,
    val description: String,
    val progressState: ProgressState = ProgressState.NotStarted,
    val readingStatus: ReadingStatus,
    val coverArt: String,
    val titleEnglish: String,
    val alternateTitles: Map<String, String>,
    val originalLanguage: String,
    val availableTranslatedLanguages: List<String>,
    val status: Status,
    val publicationDemographic: PublicationDemographic?,
    val tagToId: Map<String, String>,
    val contentRating: ContentRating,
    val lastVolume: Int,
    val lastChapter: Long,
    val version: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val savedAtLocal: LocalDateTime,
    val authors: List<String>,
    val artists: List<String>,
    val year: Int,
    val latestUploadedChapter: String?,
    val coverLastModified: Long,
    val isStub: Boolean = false
) {
    val tags = tagToId.keys.toImmutableList()
    val tagIds = tagToId.values.toImmutableList()

    companion object {
        fun stub(): Manga {
            return Manga(
                id = UUID.randomUUID().toString(),
                inLibrary = true,
                description = "",
                progressState = ProgressState.Reading,
                readingStatus = ReadingStatus.Reading,
                coverArt = "https://mangadex.org/covers/89bcfe0b-caa3-4d1b-9679-139b44a0b505/7d586570-2f39-4f73-80e5-1935e452997f.jpg",
                titleEnglish = listOf("Dr Stone", "Oshi no Bishoujo ni Kokuhaku sareru Hanashi").random(),
                alternateTitles = emptyMap(),
                originalLanguage = "en",
                availableTranslatedLanguages = listOf("en"),
                status = Status.ongoing,
                publicationDemographic = null,
                tagToId = listOf(
                    mapOf(
                    "Action" to "dfasdf",
                    "Adventure" to "dsfasdfasfds",
                    ),
                    mapOf(
                    "Survival" to "asdf",
                    "Demons" to "asdfasdfasdfasdfasdf"
                    ),
                    mapOf(
                        "Cooking" to "dkjfalksdjfkl",
                        "Romance" to "dkjfalksjdkflfdkslfj",
                        "Slice of Life" to "dkfjalksjdflkjasdlfsdsd"
                    )
                ).random(),
                contentRating = ContentRating.safe,
                lastVolume = 10,
                lastChapter = 100L,
                version = 1,
                createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                updatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                savedAtLocal = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                authors = emptyList(),
                artists = emptyList(),
                year = 2000,
                latestUploadedChapter = null,
                coverLastModified = 0L,
                isStub = true
            )
        }
    }
}


fun Manga.toResource(): MangaResource {
    val m = this
    return object : MangaResource {
        override val id: String = m.id
        override val coverArt: String = m.coverArt
        override val title: String = m.titleEnglish
        override val version: Int = m.version
        override val createdAt: LocalDateTime = m.createdAt
        override val updatedAt: LocalDateTime = m.updatedAt
    }
}
