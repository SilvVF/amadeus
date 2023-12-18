package io.silv.database.entity.manga

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.silv.common.model.ContentRating
import io.silv.common.model.MangaResource
import io.silv.common.model.ProgressState
import io.silv.common.model.PublicationDemographic
import io.silv.common.model.ReadingStatus
import io.silv.common.model.Status
import io.silv.common.time.localDateTimeNow
import io.silv.database.entity.AmadeusEntity
import kotlinx.datetime.LocalDateTime

@Entity
data class SavedMangaEntity(
    @PrimaryKey override val id: String,
    val progressState: ProgressState = ProgressState.NotStarted,
    val readingStatus: ReadingStatus,
    override val coverArt: String,
    val description: String,
    override val title: String,
    val alternateTitles: Map<String, String>,
    val originalLanguage: String,
    val availableTranslatedLanguages: List<String>,
    val status: Status,
    val tagToId: Map<String, String>,
    val contentRating: ContentRating,
    val lastVolume: Int,
    val lastChapter: Long,
    override val version: Int,
    val publicationDemographic: PublicationDemographic?,
    override val createdAt: LocalDateTime,
    override val updatedAt: LocalDateTime,
    val volumeToCoverArt: Map<String, String>,
    val savedAtLocal: LocalDateTime = localDateTimeNow(),
    val year: Int,
    val latestUploadedChapter: String?,
    val authors: List<String>,
    val artists: List<String>,
) : AmadeusEntity<String>, MangaResource {
    constructor(mangaResource: SourceMangaResource) : this(
        id = mangaResource.id,
        progressState = ProgressState.NotStarted,
        readingStatus = ReadingStatus.None,
        coverArt = mangaResource.coverArt,
        description = mangaResource.description,
        title = mangaResource.title,
        alternateTitles = mangaResource.alternateTitles,
        originalLanguage = mangaResource.originalLanguage,
        availableTranslatedLanguages = mangaResource.availableTranslatedLanguages,
        status = mangaResource.status,
        tagToId = mangaResource.tagToId,
        contentRating = mangaResource.contentRating,
        lastVolume = mangaResource.lastVolume,
        lastChapter = mangaResource.lastChapter,
        version = mangaResource.version,
        createdAt = mangaResource.createdAt,
        updatedAt = mangaResource.updatedAt,
        volumeToCoverArt = mangaResource.volumeToCoverArt,
        publicationDemographic = mangaResource.publicationDemographic,
        year = mangaResource.year,
        latestUploadedChapter = mangaResource.latestUploadedChapter,
        authors = mangaResource.authors,
        artists = mangaResource.artists,
    )
}
