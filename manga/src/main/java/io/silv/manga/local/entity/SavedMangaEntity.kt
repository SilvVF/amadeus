package io.silv.manga.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.silv.manga.repositorys.timeNow
import io.silv.manga.network.mangadex.models.ContentRating
import io.silv.manga.network.mangadex.models.PublicationDemographic
import io.silv.manga.network.mangadex.models.Status
import kotlinx.datetime.LocalDateTime

@Entity
data class SavedMangaEntity(

    @PrimaryKey override val id: String,

    val progressState: ProgressState = ProgressState.NotStarted,

    val readingStatus: ReadingStatus,

    val originalCoverArtUrl: String,

    override val coverArt: String,

    override val description: String,

    override val titleEnglish: String,

    override val alternateTitles: Map<String, String>,

    override val originalLanguage: String,

    override val availableTranslatedLanguages: List<String>,

    override val status: Status,

    override val tagToId: Map<String, String>,

    override val contentRating: ContentRating,

    override val lastVolume: Int,

    override val lastChapter: Long,

    override val version: Int,

    val bookmarked: Boolean,

    override val publicationDemographic: PublicationDemographic?,

    override val createdAt: LocalDateTime,

    override val updatedAt: LocalDateTime,

    override val volumeToCoverArt: Map<String, String>,

    override val savedAtLocal: LocalDateTime = timeNow(),

    override val year: Int,

    override val latestUploadedChapter: String?,

    override val authors: List<String>,

    override val artists: List<String>

): AmadeusEntity<Any?>, MangaResource {
    constructor(mangaResource: MangaResource): this(
        id = mangaResource.id,
        progressState = ProgressState.NotStarted,
        readingStatus = ReadingStatus.None,
        originalCoverArtUrl = mangaResource.coverArt,
        coverArt = "",
        description = mangaResource.description,
        titleEnglish = mangaResource.titleEnglish,
        alternateTitles = mangaResource.alternateTitles,
        originalLanguage = mangaResource.originalLanguage,
        availableTranslatedLanguages = mangaResource.availableTranslatedLanguages,
        status = mangaResource.status,
        tagToId = mangaResource.tagToId,
        contentRating = mangaResource.contentRating,
        lastVolume = mangaResource.lastVolume,
        lastChapter = mangaResource.lastChapter,
        version = mangaResource.version,
        bookmarked = false,
        createdAt = mangaResource.createdAt,
        updatedAt = mangaResource.updatedAt,
        volumeToCoverArt = emptyMap(),
        publicationDemographic = mangaResource.publicationDemographic,
        year = mangaResource.year,
        latestUploadedChapter = mangaResource.latestUploadedChapter,
        authors = mangaResource.authors,
        artists = mangaResource.artists
    )

    constructor(
        mangaResources: List<MangaResource>,
        recent: MangaResource = mangaResources.maxBy { it.savedAtLocal }
    ): this(
            id = recent.id,
            progressState = ProgressState.NotStarted,
            readingStatus = ReadingStatus.None,
            coverArt = "",
            description = recent.description,
            titleEnglish = recent.titleEnglish,
            originalCoverArtUrl = recent.coverArt,
            alternateTitles = recent.alternateTitles,
            originalLanguage = recent.originalLanguage,
            availableTranslatedLanguages = recent.availableTranslatedLanguages,
            status = recent.status,
            tagToId = recent.tagToId,
            contentRating = recent.contentRating,
            lastVolume = recent.lastVolume,
            lastChapter = recent.lastChapter,
            version = recent.version,
            bookmarked = false,
            createdAt = recent.createdAt,
            updatedAt = recent.updatedAt,
            publicationDemographic = recent.publicationDemographic,
            volumeToCoverArt = mangaResources
                .filter { it.volumeToCoverArt.isNotEmpty() }
                .maxBy { it.savedAtLocal }
                .volumeToCoverArt,
            latestUploadedChapter = recent.latestUploadedChapter,
            year = recent.year,
            authors = recent.authors,
            artists = recent.artists
        )
}
