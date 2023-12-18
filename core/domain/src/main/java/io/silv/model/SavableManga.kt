package io.silv.model

import androidx.compose.runtime.Stable
import io.silv.common.model.ContentRating
import io.silv.common.model.MangaResource
import io.silv.common.model.ProgressState
import io.silv.common.model.PublicationDemographic
import io.silv.common.model.ReadingStatus
import io.silv.common.model.Status
import io.silv.database.entity.manga.SavedMangaEntity
import io.silv.database.entity.manga.SourceMangaResource
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
    val volumeToCoverArtUrl: Map<String, String>,
    val authors: List<String>,
    val artists: List<String>,
    val year: Int,
) {
    val tags = tagToId.keys.toImmutableList()
    val tagIds = tagToId.values.toImmutableList()

    constructor(savedManga: SavedMangaEntity) : this(
        id = savedManga.id,
        inLibrary = true,
        description = savedManga.description,
        progressState = savedManga.progressState,
        coverArt = savedManga.coverArt,
        titleEnglish = savedManga.title,
        alternateTitles = savedManga.alternateTitles,
        originalLanguage = savedManga.originalLanguage,
        availableTranslatedLanguages = savedManga.availableTranslatedLanguages.toImmutableList(),
        status = savedManga.status,
        tagToId = savedManga.tagToId,
        contentRating = savedManga.contentRating,
        lastVolume = savedManga.lastVolume,
        lastChapter = savedManga.lastChapter,
        version = savedManga.version,
        createdAt = savedManga.createdAt,
        updatedAt = savedManga.updatedAt,
        savedLocalAtEpochSeconds = savedManga.savedAtLocal,
        volumeToCoverArtUrl = savedManga.volumeToCoverArt,
        publicationDemographic = savedManga.publicationDemographic,
        readingStatus = savedManga.readingStatus,
        year = savedManga.year,
        artists = savedManga.artists,
        authors = savedManga.authors,
    )
    constructor(mangaResource: SourceMangaResource) : this(
        id = mangaResource.id,
        inLibrary = false,
        description = mangaResource.description,
        progressState = mangaResource.progressState,
        coverArt = mangaResource.coverArt,
        titleEnglish = mangaResource.title,
        alternateTitles = mangaResource.alternateTitles,
        originalLanguage = mangaResource.originalLanguage,
        availableTranslatedLanguages = mangaResource.availableTranslatedLanguages.toImmutableList(),
        status = mangaResource.status,
        tagToId = mangaResource.tagToId,
        contentRating = mangaResource.contentRating,
        lastVolume = mangaResource.lastVolume,
        lastChapter = mangaResource.lastChapter,
        version = mangaResource.version,
        createdAt = mangaResource.createdAt,
        updatedAt = mangaResource.updatedAt,
        savedLocalAtEpochSeconds = mangaResource.savedAtLocal,
        volumeToCoverArtUrl = mangaResource.volumeToCoverArt,
        publicationDemographic = mangaResource.publicationDemographic,
        readingStatus = mangaResource.readingStatus,
        year = mangaResource.year,
        artists = mangaResource.artists,
        authors = mangaResource.authors,
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
