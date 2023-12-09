package io.silv

import io.silv.database.entity.manga.MangaResource
import io.silv.database.entity.manga.SavedMangaEntity
import io.silv.model.SavableManga
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object DateTimeAsLongSerializer : KSerializer<kotlinx.datetime.LocalDateTime> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: kotlinx.datetime.LocalDateTime) {
        encoder.encodeLong(value.toInstant(TimeZone.currentSystemDefault()).epochSeconds)
    }

    override fun deserialize(decoder: Decoder): kotlinx.datetime.LocalDateTime {
        return Instant.fromEpochSeconds(decoder.decodeLong()).toLocalDateTime(TimeZone.currentSystemDefault())
    }
}

fun SavedMangaEntity.toSavable(
    mangaResources: List<MangaResource>?,
    newest: MangaResource? = mangaResources?.maxByOrNull { it.savedAtLocal }
): SavableManga {
    return SavableManga(
        id = this.id,
        bookmarked = true,
        description = newest?.description ?: this.description,
        progressState = this.progressState,
        coverArt = this.coverArt.ifEmpty { null } ?: newest?.coverArt ?: this.originalCoverArtUrl,
        titleEnglish = newest?.titleEnglish ?: this.titleEnglish,
        alternateTitles = newest?.alternateTitles ?: this.alternateTitles,
        originalLanguage = newest?.originalLanguage ?: this.originalLanguage,
        availableTranslatedLanguages = newest?.availableTranslatedLanguages?.toImmutableList()
            ?: this.availableTranslatedLanguages.toImmutableList(),
        status = newest?.status ?: this.status,
        tagToId = newest?.tagToId ?: this.tagToId,
        contentRating = newest?.contentRating ?: this.contentRating,
        lastVolume = newest?.lastVolume ?: this.lastVolume,
        lastChapter = newest?.lastChapter ?: this.lastChapter,
        version = newest?.version ?: this.version,
        createdAt = newest?.createdAt ?: this.createdAt,
        updatedAt = newest?.updatedAt ?: this.updatedAt,
        savedLocalAtEpochSeconds = newest?.savedAtLocal ?: this.savedAtLocal,
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