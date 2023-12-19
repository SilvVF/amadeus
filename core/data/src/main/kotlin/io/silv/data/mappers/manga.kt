package io.silv.data.mappers

import io.silv.common.model.ProgressState
import io.silv.common.model.ReadingStatus
import io.silv.common.time.localDateTimeNow
import io.silv.common.time.parseMangaDexTimeToDateTime
import io.silv.database.entity.manga.MangaEntity
import io.silv.network.model.manga.Manga


fun Manga.toEntity(): MangaEntity {
    return MangaEntity(
        id = id,
        favorite = false,
        coverArt = coverArtUrl(this),
        description = descriptionEnglish,
        title = titleEnglish,
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
        savedAtLocal = localDateTimeNow(),
        year = attributes.year ?: -1,
        latestUploadedChapter = attributes.latestUploadedChapter,
        authors = authors,
        artists = artists,
        progressState = ProgressState.NotStarted,
        readingStatus = ReadingStatus.None
    )
}
