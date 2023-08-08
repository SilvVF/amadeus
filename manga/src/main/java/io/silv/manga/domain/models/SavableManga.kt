package io.silv.manga.domain.models

import android.os.Parcel
import android.os.Parcelable
import io.silv.manga.local.entity.MangaResource
import io.silv.manga.local.entity.ProgressState
import io.silv.manga.local.entity.ReadingStatus
import io.silv.manga.local.entity.SavedMangaEntity
import io.silv.manga.network.mangadex.models.ContentRating
import io.silv.manga.network.mangadex.models.PublicationDemographic
import io.silv.manga.network.mangadex.models.Status
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import java.io.Serializable

@Parcelize
data class SavableManga(
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
    val lastVolume: Int,
    val lastChapter: Long,
    val version: Int,
    @TypeParceler<LocalDateTime, LocalDateTimeParceler>
    val createdAt: LocalDateTime,
    @TypeParceler<LocalDateTime, LocalDateTimeParceler>
    val updatedAt: LocalDateTime,
    @TypeParceler<LocalDateTime, LocalDateTimeParceler>
    val savedLocalAtEpochSeconds: LocalDateTime,
    val volumeToCoverArtUrl: Map<String, String>,
    val readChapters: List<String>,
    val chapterToLastReadPage: Map<String, Int>,
    val authors: List<String>,
    val artists: List<String>,
    val year: Int,
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
        savedLocalAtEpochSeconds = savedManga.savedAtLocal,
        volumeToCoverArtUrl = savedManga.volumeToCoverArt,
        readChapters = savedManga.readChapters,
        chapterToLastReadPage = savedManga.chapterToLastReadPage,
        publicationDemographic = savedManga.publicationDemographic,
        readingStatus = savedManga.readingStatus,
        year = savedManga.year,
        artists = savedManga.artists,
        authors = savedManga.authors
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
        savedLocalAtEpochSeconds = mangaResource.savedAtLocal,
        volumeToCoverArtUrl = savedManga?.volumeToCoverArt ?: emptyMap(),
        readChapters = savedManga?.readChapters ?: emptyList(),
        chapterToLastReadPage = savedManga?.chapterToLastReadPage ?: emptyMap(),
        publicationDemographic = mangaResource.publicationDemographic,
        readingStatus = savedManga?.readingStatus ?: ReadingStatus.None,
        year = mangaResource.year,
        artists = mangaResource.artists,
        authors = mangaResource.authors
    )
    constructor(
        mangaResources: List<MangaResource>,
        savedManga: SavedMangaEntity?,
        newest: MangaResource = mangaResources.maxBy { it.savedAtLocal }
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
        savedLocalAtEpochSeconds = newest.savedAtLocal,
        volumeToCoverArtUrl = buildMap {
            savedManga?.volumeToCoverArt?.let { putAll(it) }
            mangaResources.map { it.volumeToCoverArt }.forEach { putAll(it) }
        },
        readChapters = savedManga?.readChapters ?: emptyList(),
        chapterToLastReadPage = savedManga?.chapterToLastReadPage ?: emptyMap(),
        publicationDemographic = newest.publicationDemographic,
        readingStatus = savedManga?.readingStatus ?: ReadingStatus.None,
        year = newest.year,
        authors = newest.authors,
        artists = newest.artists
    )

}

object LocalDateTimeParceler : Parceler<LocalDateTime> {
    override fun create(parcel: Parcel): LocalDateTime {
        val date = parcel.readLong()
        return Instant.fromEpochSeconds(date).toLocalDateTime(TimeZone.currentSystemDefault())
    }

    override fun LocalDateTime.write(parcel: Parcel, flags: Int) {
        parcel.writeLong(this.toInstant(TimeZone.currentSystemDefault()).epochSeconds)
    }
}