package io.silv.manga.domain.models

import android.os.Parcelable
import io.silv.manga.local.entity.MangaResource
import io.silv.manga.local.entity.ProgressState
import io.silv.manga.local.entity.ReadingStatus
import io.silv.manga.local.entity.SavedMangaEntity
import io.silv.manga.network.mangadex.models.ContentRating
import io.silv.manga.network.mangadex.models.PublicationDemographic
import io.silv.manga.network.mangadex.models.Status
import kotlinx.datetime.Clock
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
data class DomainManga(
    val id: String,
    val bookmarked: Boolean = false,
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
    val lastVolume: String? = null,
    val lastChapter: String? = null,
    val version: Int,
    val createdAt: String,
    val updatedAt: String,
    val savedLocalAtEpochSeconds: Long = Clock.System.now().epochSeconds,
    val volumeToCoverArtUrl: Map<String, String>,
    val readChapters: List<String>,
    val chapterToLastReadPage: Map<String, Int>,

    ): Parcelable, Serializable {
    constructor(savedManga: SavedMangaEntity) : this(
        id = savedManga.id,
        bookmarked = savedManga.bookmarked,
        description = savedManga.description,
        progressState = savedManga.progressState ,
        coverArt = savedManga.coverArt,
        titleEnglish = savedManga.titleEnglish,
        alternateTitles = savedManga.alternateTitles,
        originalLanguage = savedManga.originalLanguage,
        availableTranslatedLanguages = savedManga.availableTranslatedLanguages,
        status = savedManga.status,
        tagToId = savedManga.tagToId,
        contentRating = savedManga.contentRating,
        lastVolume = savedManga.lastVolume,
        lastChapter = savedManga.lastChapter,
        version = savedManga.version,
        createdAt = savedManga.createdAt,
        updatedAt = savedManga.updatedAt,
        savedLocalAtEpochSeconds = savedManga.savedLocalAtEpochSeconds,
        volumeToCoverArtUrl = savedManga.volumeToCoverArt,
        readChapters = savedManga.readChapters,
        chapterToLastReadPage = savedManga.chapterToLastReadPage,
        publicationDemographic = savedManga.publicationDemographic,
        readingStatus = savedManga.readingStatus
    )
    constructor(mangaResource: MangaResource, savedManga: SavedMangaEntity?) : this(
        id = mangaResource.id,
        bookmarked = savedManga?.bookmarked ?: false,
        description = mangaResource.description,
        progressState = savedManga?.progressState ?: ProgressState.NotStarted,
        coverArt = mangaResource.coverArt,
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
        createdAt = mangaResource.createdAt,
        updatedAt = mangaResource.updatedAt,
        savedLocalAtEpochSeconds = mangaResource.savedLocalAtEpochSeconds,
        volumeToCoverArtUrl = savedManga?.volumeToCoverArt ?: emptyMap(),
        readChapters = savedManga?.readChapters ?: emptyList(),
        chapterToLastReadPage = savedManga?.chapterToLastReadPage ?: emptyMap(),
        publicationDemographic = mangaResource.publicationDemographic,
        readingStatus = savedManga?.readingStatus ?: ReadingStatus.None
    )
    constructor(
        mangaResources: List<MangaResource>,
        savedManga: SavedMangaEntity?,
        newest: MangaResource = mangaResources.maxBy { it.savedLocalAtEpochSeconds }
    ) : this(
        id = newest.id,
        bookmarked = savedManga?.bookmarked ?: false,
        description = newest.description,
        progressState = savedManga?.progressState ?: ProgressState.NotStarted,
        coverArt = newest.coverArt,
        titleEnglish = newest.titleEnglish,
        alternateTitles = newest.alternateTitles,
        originalLanguage = newest.originalLanguage,
        availableTranslatedLanguages = newest.availableTranslatedLanguages,
        status = newest.status,
        tagToId = newest.tagToId,
        contentRating = newest.contentRating,
        lastVolume = newest.lastVolume,
        lastChapter = newest.lastChapter,
        version = newest.version,
        createdAt = newest.createdAt,
        updatedAt = newest.updatedAt,
        savedLocalAtEpochSeconds = newest.savedLocalAtEpochSeconds,
        volumeToCoverArtUrl = buildMap {
            savedManga?.volumeToCoverArt?.let { putAll(it) }
            mangaResources.map { it.volumeToCoverArt }.forEach { putAll(it) }
        },
        readChapters = savedManga?.readChapters ?: emptyList(),
        chapterToLastReadPage = savedManga?.chapterToLastReadPage ?: emptyMap(),
        publicationDemographic = newest.publicationDemographic,
        readingStatus = savedManga?.readingStatus ?: ReadingStatus.None
    )
}