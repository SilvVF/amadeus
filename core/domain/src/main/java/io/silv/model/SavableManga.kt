package io.silv.model

import androidx.compose.runtime.Stable
import io.silv.common.model.ContentRating
import io.silv.common.model.MangaResource
import io.silv.common.model.ProgressState
import io.silv.common.model.PublicationDemographic
import io.silv.common.model.ReadingStatus
import io.silv.common.model.Status
import io.silv.database.entity.manga.MangaEntity
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.LocalDateTime

@Stable
data class SavableManga(
    val id: String,
    val inLibrary: Boolean,
    val description: String,
    val progressState: ProgressState = ProgressState.NotStarted,
    val readingStatus: ReadingStatus,
    val coverArt: String,
    val titleEnglish: String,
    val alternateTitles: Map<String, String>,
    val originalLanguage: String,
    val availableTranslatedLanguages: ImmutableList<String>,
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
) {
    val tags = tagToId.keys.toImmutableList()
    val tagIds = tagToId.values.toImmutableList()

    constructor(entity: MangaEntity) : this(
        id = entity.id,
        inLibrary = entity.favorite,
        description = entity.description,
        progressState = entity.progressState,
        coverArt = entity.coverArt,
        titleEnglish = entity.title,
        alternateTitles = entity.alternateTitles,
        originalLanguage = entity.originalLanguage,
        availableTranslatedLanguages = entity.availableTranslatedLanguages.toImmutableList(),
        status = entity.status,
        tagToId = entity.tagToId,
        contentRating = entity.contentRating,
        lastVolume = entity.lastVolume,
        lastChapter = entity.lastChapter,
        version = entity.version,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        savedLocalAtEpochSeconds = entity.savedAtLocal,
        publicationDemographic = entity.publicationDemographic,
        readingStatus = entity.readingStatus,
        year = entity.year,
        artists = entity.artists,
        authors = entity.authors,
    )
}

fun SavableManga.toResource(): MangaResource {
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
