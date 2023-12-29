package io.silv.domain.manga.model

import androidx.compose.runtime.Stable
import io.silv.common.model.ContentRating
import io.silv.common.model.MangaResource
import io.silv.common.model.ProgressState
import io.silv.common.model.PublicationDemographic
import io.silv.common.model.ReadingStatus
import io.silv.common.model.Status
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.LocalDateTime

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
    val savedLocalAtEpochSeconds: LocalDateTime,
    val authors: List<String>,
    val artists: List<String>,
    val year: Int,
    val latestUploadedChapter: String?,
) {
    val tags = tagToId.keys.toImmutableList()
    val tagIds = tagToId.values.toImmutableList()

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
