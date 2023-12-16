package io.silv.data.mappers

import io.silv.common.model.ProgressState
import io.silv.common.model.ReadingStatus
import io.silv.common.time.localDateTimeNow
import io.silv.common.time.parseMangaDexTimeToDateTime
import io.silv.database.entity.manga.SavedMangaEntity
import io.silv.database.entity.manga.SourceMangaResource
import io.silv.network.model.manga.Manga


fun Manga.toSourceManga(): SourceMangaResource {
    return SourceMangaResource(
        id = id,
        coverArt = coverArtUrl(this),
        description = descriptionEnglish,
        titleEnglish = titleEnglish,
        alternateTitles = alternateTitles,
        originalLanguage = attributes.originalLanguage,
        availableTranslatedLanguages = attributes.availableTranslatedLanguages.filterNotNull(),
        status = attributes.status,
        tagToId = tagToId,
        contentRating = attributes.contentRating,
        lastVolume = attributes.lastVolume?.toIntOrNull() ?: -1,
        lastChapter = attributes.lastChapter?.toLongOrNull() ?: -1L,
        version = attributes.version,
        createdAt = attributes.createdAt.parseMangaDexTimeToDateTime(),
        updatedAt = attributes.updatedAt.parseMangaDexTimeToDateTime(),
        publicationDemographic = attributes.publicationDemographic,
        volumeToCoverArt = emptyMap(),
        savedAtLocal = localDateTimeNow(),
        year = attributes.year ?: -1,
        latestUploadedChapter = attributes.latestUploadedChapter,
        authors = authors,
        artists = artists,
        progressState = ProgressState.NotStarted,
        readingStatus = ReadingStatus.None
    )
}

fun Manga.toSavedManga(saved: SavedMangaEntity? = null): SavedMangaEntity {
    val network = this
    return SavedMangaEntity(
        id = network.id,
        progressState = saved?.progressState ?: ProgressState.NotStarted,
        description = network.descriptionEnglish,
        titleEnglish = network.titleEnglish,
        alternateTitles = network.alternateTitles,
        originalLanguage = network.attributes.originalLanguage,
        availableTranslatedLanguages = network.attributes.availableTranslatedLanguages
            .filterNotNull(),
        status = network.attributes.status,
        contentRating = network.attributes.contentRating,
        lastVolume = network.attributes.lastVolume?.toIntOrNull() ?: - 1,
        lastChapter = network.attributes.lastChapter?.toLongOrNull() ?: -1L,
        version = network.attributes.version,
        volumeToCoverArt = saved?.volumeToCoverArt ?: emptyMap(),
        createdAt = network.attributes.createdAt.parseMangaDexTimeToDateTime(),
        updatedAt = network.attributes.updatedAt.parseMangaDexTimeToDateTime(),
        tagToId = network.tagToId,
        publicationDemographic = network.attributes.publicationDemographic,
        readingStatus = saved?.readingStatus ?: ReadingStatus.None,
        authors = network.authors,
        artists = network.artists,
        year = network.attributes.year ?: -1,
        latestUploadedChapter = network.attributes.latestUploadedChapter,
        coverArt = saved?.coverArt ?: coverArtUrl(this)
    )
}
