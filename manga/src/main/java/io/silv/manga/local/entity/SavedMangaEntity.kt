package io.silv.manga.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.silv.manga.network.mangadex.models.ContentRating
import io.silv.manga.network.mangadex.models.PublicationDemographic
import io.silv.manga.network.mangadex.models.Status
import kotlinx.datetime.Clock

@Entity
data class SavedMangaEntity(

    @PrimaryKey override val id: String,

    val progressState: ProgressState = ProgressState.NotStarted,

    val readChapters: List<String>,

    val chapterToLastReadPage: Map<String, Int>,

    val readingStatus: ReadingStatus,

    override val coverArt: String,

    override val description: String,

    override val titleEnglish: String,

    override val alternateTitles: Map<String, String>,

    override val originalLanguage: String,

    override val availableTranslatedLanguages: List<String>,

    override val status: Status,

    override val tagToId: Map<String, String>,

    override val contentRating: ContentRating,

    override val lastVolume: String? = null,

    override val lastChapter: String? = null,

    override val version: Int,

    val bookmarked: Boolean,

    override val publicationDemographic: PublicationDemographic?,

    override val createdAt: String,

    override val updatedAt: String,

    override val volumeToCoverArt: Map<String, String>,

    override val savedLocalAtEpochSeconds: Long = Clock.System.now().epochSeconds
): AmadeusEntity<Any?>, MangaResource {
    constructor(mangaResource: MangaResource): this(
        id = mangaResource.id,
        progressState = ProgressState.NotStarted,
        chapterToLastReadPage = emptyMap(),
        readChapters = emptyList(),
        readingStatus = ReadingStatus.None,
        coverArt = mangaResource.coverArt,
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
        publicationDemographic = mangaResource.publicationDemographic
    )

    constructor(
        mangaResources: List<MangaResource>,
        recent: MangaResource = mangaResources.maxBy { it.savedLocalAtEpochSeconds }
    ): this(
            id = recent.id,
            progressState = ProgressState.NotStarted,
            chapterToLastReadPage = emptyMap(),
            readingStatus = ReadingStatus.None,
            readChapters = emptyList(),
            coverArt = recent.coverArt,
            description = recent.description,
            titleEnglish = recent.titleEnglish,
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
                .maxBy { it.savedLocalAtEpochSeconds }
                .volumeToCoverArt,
        )
}
