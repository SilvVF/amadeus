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
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

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
    @Serializable(with=DateTimeAsLongSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with=DateTimeAsLongSerializer::class)
    val updatedAt: LocalDateTime,
    @Serializable(with=DateTimeAsLongSerializer::class)
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
    constructor(mangaResource: MangaResource, savedManga: SavedMangaEntity?) : this(
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


    constructor(
        mangaResources: List<MangaResource>,
        savedManga: SavedMangaEntity?,
        newest: MangaResource = mangaResources.maxBy { it.savedAtLocal }
    ) : this(
        id = newest.id,
        bookmarked = savedManga?.bookmarked ?: false,
        description = newest.description,
        progressState = savedManga?.progressState ?: ProgressState.NotStarted,
        coverArt = savedManga?.coverArt?.ifEmpty { null } ?: newest.coverArt,
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
        publicationDemographic = newest.publicationDemographic,
        readingStatus = savedManga?.readingStatus ?: ReadingStatus.None,
        year = newest.year,
        authors = newest.authors,
        artists = newest.artists
    )
}

fun SavedMangaEntity.toSavable(
    mangaResources: List<MangaResource>?,
    newest: MangaResource? = mangaResources?.maxByOrNull { it.savedAtLocal }
): SavableManga {
    return SavableManga(
    id = this.id,
    bookmarked = this.bookmarked ,
    description = newest?.description ?: this.description,
    progressState = this.progressState,
    coverArt = this.coverArt.ifEmpty { null } ?: newest?.coverArt ?: this.originalCoverArtUrl,
    titleEnglish = newest?.titleEnglish ?: this.titleEnglish,
    alternateTitles = newest?.alternateTitles ?: this.alternateTitles,
    originalLanguage = newest?.originalLanguage ?: this.originalLanguage,
    availableTranslatedLanguages = newest?.availableTranslatedLanguages ?: this.availableTranslatedLanguages,
    status = newest?.status ?: this.status,
    tagToId = newest?.tagToId ?: this.tagToId,
    contentRating = newest?.contentRating ?: this.contentRating,
    lastVolume = newest?.lastVolume ?: this.lastVolume,
    lastChapter = newest?.lastChapter ?: this.lastChapter,
    version = newest?.version ?: this.version,
    createdAt = newest?.createdAt ?: this.createdAt,
    updatedAt = newest?.updatedAt ?: this.updatedAt,
    savedLocalAtEpochSeconds = newest?.savedAtLocal ?: this.savedAtLocal  ,
    volumeToCoverArtUrl = buildMap {
        putAll(this@toSavable.volumeToCoverArt)
        mangaResources?.map { it.volumeToCoverArt }?.forEach { putAll(it) }
    },
    publicationDemographic = newest?.publicationDemographic,
    readingStatus = this.readingStatus,
    year = newest?.year ?: this.year,
    authors = newest?.authors ?: this.authors,
    artists = newest?.artists ?: this.artists
    )
}


object DateTimeAsLongSerializer : KSerializer<LocalDateTime> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeLong(value.toInstant(TimeZone.currentSystemDefault()).epochSeconds)
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return Instant.fromEpochSeconds(decoder.decodeLong()).toLocalDateTime(TimeZone.currentSystemDefault())
    }
}

