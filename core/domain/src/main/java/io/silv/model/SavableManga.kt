package io.silv.model

import android.os.Parcel
import android.os.Parcelable
import io.silv.DateTimeAsLongSerializer
import io.silv.common.model.ContentRating
import io.silv.common.model.ProgressState
import io.silv.common.model.PublicationDemographic
import io.silv.common.model.ReadingStatus
import io.silv.common.model.Status
import io.silv.database.entity.manga.SavedMangaEntity
import io.silv.database.entity.manga.SourceMangaResource
import kotlinx.datetime.LocalDateTime
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

@Parcelize
@Serializable
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
    @Serializable(with= DateTimeAsLongSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with= DateTimeAsLongSerializer::class)
    val updatedAt: LocalDateTime,
    @Serializable(with= DateTimeAsLongSerializer::class)
    val savedLocalAtEpochSeconds: LocalDateTime,
    val volumeToCoverArtUrl: Map<String, String>,
    val authors: List<String>,
    val artists: List<String>,
    val year: Int,
): Parcelable {

    companion object : kotlinx.parcelize.Parceler<SavableManga>   {
        override fun SavableManga.write(parcel: Parcel, flags: Int) {
            parcel.writeString(
                Json.encodeToString(
                    serializer(),
                    this
                )
            )
        }

        override fun create(parcel: Parcel): SavableManga {
            return Json.decodeFromString(serializer(), parcel.readString() ?: "")
        }
    }

    constructor(savedManga: SavedMangaEntity) : this(
        id = savedManga.id,
        bookmarked = savedManga.bookmarked,
        description = savedManga.description,
        progressState = savedManga.progressState,
        coverArt = savedManga.coverArt.ifEmpty { savedManga.originalCoverArtUrl },
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
        publicationDemographic = savedManga.publicationDemographic,
        readingStatus = savedManga.readingStatus,
        year = savedManga.year,
        artists = savedManga.artists,
        authors = savedManga.authors
    )
    constructor(mangaResource: SourceMangaResource, savedManga: SavedMangaEntity?) : this(
        id = mangaResource.id,
        bookmarked = savedManga?.bookmarked ?: false,
        description = mangaResource.description,
        progressState = savedManga?.progressState ?: ProgressState.NotStarted,
        coverArt = savedManga?.coverArt?.ifEmpty { null } ?: mangaResource.coverArt,
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
        publicationDemographic = mangaResource.publicationDemographic,
        readingStatus = savedManga?.readingStatus ?: ReadingStatus.None,
        year = mangaResource.year,
        artists = mangaResource.artists,
        authors = mangaResource.authors
    )
}