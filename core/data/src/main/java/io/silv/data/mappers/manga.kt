package io.silv.data.mappers

import io.silv.common.model.ProgressState
import io.silv.common.model.ReadingStatus
import io.silv.common.time.localDateTimeNow
import io.silv.common.time.parseMangaDexTimeToDateTime
import io.silv.database.entity.manga.SavedMangaEntity
import io.silv.database.entity.manga.resource.FilteredMangaResource
import io.silv.database.entity.manga.resource.FilteredMangaYearlyResource
import io.silv.database.entity.manga.resource.PopularMangaResource
import io.silv.database.entity.manga.resource.QuickSearchMangaResource
import io.silv.database.entity.manga.resource.RecentMangaResource
import io.silv.database.entity.manga.resource.SearchMangaResource
import io.silv.database.entity.manga.resource.SeasonalMangaResource
import io.silv.database.entity.manga.resource.TempMangaResource
import io.silv.network.model.manga.Manga

fun Manga.toSeasonalMangaResource(): SeasonalMangaResource {
    return SeasonalMangaResource(
        id = id,
        description = descriptionEnglish,
        coverArt = coverArtUrl(this),
        titleEnglish = titleEnglish,
        alternateTitles = alternateTitles,
        originalLanguage = attributes.originalLanguage,
        availableTranslatedLanguages = attributes.availableTranslatedLanguages
            .filterNotNull(),
        status = attributes.status,
        contentRating = attributes.contentRating,
        lastVolume = attributes.lastVolume?.toIntOrNull() ?: -1,
        lastChapter = attributes.lastChapter?.toLongOrNull() ?: -1L,
        version = attributes.version,
        createdAt = attributes.createdAt.parseMangaDexTimeToDateTime(),
        updatedAt = attributes.updatedAt.parseMangaDexTimeToDateTime(),
        tagToId = tagToId,
        seasonId = "",
        publicationDemographic = attributes.publicationDemographic,
        latestUploadedChapter = attributes.latestUploadedChapter,
        year = attributes.year ?: -1,
        authors = authors,
        artists = artists
    )
}


fun Manga.toTempMangaResource(): TempMangaResource {
    return TempMangaResource(
        id = id,
        description = descriptionEnglish,
        coverArt = coverArtUrl(this ),
        titleEnglish = titleEnglish,
        alternateTitles = alternateTitles,
        originalLanguage = attributes.originalLanguage,
        availableTranslatedLanguages = attributes.availableTranslatedLanguages
            .filterNotNull(),
        status = attributes.status,
        contentRating = attributes.contentRating,
        lastVolume = attributes.lastVolume?.toIntOrNull() ?: -1,
        lastChapter = attributes.lastChapter?.toLongOrNull() ?: -1L,
        version = attributes.version,
        createdAt = attributes.createdAt.parseMangaDexTimeToDateTime(),
        updatedAt = attributes.updatedAt.parseMangaDexTimeToDateTime(),
        tagToId = tagToId,
        publicationDemographic = attributes.publicationDemographic,
        latestUploadedChapter = attributes.latestUploadedChapter,
        year = attributes.year ?: -1,
        authors = authors,
        artists = artists
    )
}

fun Manga.toPopularMangaResource(): PopularMangaResource {
    return  PopularMangaResource(
        id = id,
        description = descriptionEnglish,
        coverArt = coverArtUrl(this ),
        titleEnglish = titleEnglish,
        alternateTitles = alternateTitles,
        originalLanguage = attributes.originalLanguage,
        availableTranslatedLanguages = attributes.availableTranslatedLanguages
            .filterNotNull(),
        status = attributes.status,
        contentRating = attributes.contentRating,
        lastVolume = attributes.lastVolume?.toIntOrNull() ?: -1,
        lastChapter = attributes.lastChapter?.toLongOrNull() ?: -1L,
        version = attributes.version,
        createdAt = attributes.createdAt.parseMangaDexTimeToDateTime(),
        updatedAt = attributes.updatedAt.parseMangaDexTimeToDateTime(),
        tagToId = tagToId,
        publicationDemographic = attributes.publicationDemographic,
        latestUploadedChapter = attributes.latestUploadedChapter,
        year = attributes.year ?: -1,
        authors = authors,
        artists = artists
    )
}

fun Manga.toQuickSearchMangaResource(): QuickSearchMangaResource {
    return  QuickSearchMangaResource(
        id = id,
        description = descriptionEnglish,
        coverArt = coverArtUrl(this ),
        titleEnglish = titleEnglish,
        alternateTitles = alternateTitles,
        originalLanguage = attributes.originalLanguage,
        availableTranslatedLanguages = attributes.availableTranslatedLanguages
            .filterNotNull(),
        status = attributes.status,
        contentRating = attributes.contentRating,
        lastVolume = attributes.lastVolume?.toIntOrNull() ?: -1,
        lastChapter = attributes.lastChapter?.toLongOrNull() ?: -1L,
        version = attributes.version,
        createdAt = attributes.createdAt.parseMangaDexTimeToDateTime(),
        updatedAt = attributes.updatedAt.parseMangaDexTimeToDateTime(),
        tagToId = tagToId,
        publicationDemographic = attributes.publicationDemographic,
        latestUploadedChapter = attributes.latestUploadedChapter,
        year = attributes.year ?: -1,
        authors = authors,
        artists = artists
    )
}

fun Manga.toSearchMangaResource(): SearchMangaResource {
    return  SearchMangaResource(
        id = id,
        description = descriptionEnglish,
        coverArt = coverArtUrl(this ),
        titleEnglish = titleEnglish,
        alternateTitles = alternateTitles,
        originalLanguage = attributes.originalLanguage,
        availableTranslatedLanguages = attributes.availableTranslatedLanguages
            .filterNotNull(),
        status = attributes.status,
        contentRating = attributes.contentRating,
        lastVolume = attributes.lastVolume?.toIntOrNull() ?: -1,
        lastChapter = attributes.lastChapter?.toLongOrNull() ?: -1L,
        version = attributes.version,
        createdAt = attributes.createdAt.parseMangaDexTimeToDateTime(),
        updatedAt = attributes.updatedAt.parseMangaDexTimeToDateTime(),
        tagToId = tagToId,
        publicationDemographic = attributes.publicationDemographic,
        latestUploadedChapter = attributes.latestUploadedChapter,
        year = attributes.year ?: -1,
        authors = authors,
        artists = artists
    )
}

fun Manga.toFilteredMangaYearlyResource(prev: FilteredMangaYearlyResource? = null): FilteredMangaYearlyResource {
    return  FilteredMangaYearlyResource(
        id = id,
        description = descriptionEnglish,
        coverArt = coverArtUrl(this),
        titleEnglish = titleEnglish,
        alternateTitles = alternateTitles,
        originalLanguage = attributes.originalLanguage,
        availableTranslatedLanguages = attributes.availableTranslatedLanguages.filterNotNull(),
        status = attributes.status,
        contentRating = attributes.contentRating,
        lastVolume = attributes.lastVolume?.toIntOrNull() ?: -1,
        lastChapter = attributes.lastChapter?.toLongOrNull() ?: -1L,
        version = attributes.version,
        createdAt = attributes.createdAt.parseMangaDexTimeToDateTime(),
        updatedAt = attributes.updatedAt.parseMangaDexTimeToDateTime(),
        tagToId = tagToId,
        savedAtLocal = localDateTimeNow(),
        topTags = prev?.topTags ?: emptyList(),
        topTagPlacement = prev?.topTagPlacement ?: emptyMap(),
        publicationDemographic = attributes.publicationDemographic,
        latestUploadedChapter = attributes.latestUploadedChapter,
        year = attributes.year ?: -1,
        authors = authors,
        artists = artists
    )
}

fun Manga.toFilteredMangaResource(): FilteredMangaResource {
    return FilteredMangaResource(
        id = id,
        description = descriptionEnglish,
        coverArt = coverArtUrl(this),
        titleEnglish = titleEnglish,
        alternateTitles = alternateTitles,
        originalLanguage = attributes.originalLanguage,
        availableTranslatedLanguages = attributes.availableTranslatedLanguages
            .filterNotNull(),
        status = attributes.status,
        contentRating = attributes.contentRating,
        lastVolume = attributes.lastVolume?.toIntOrNull() ?: -1,
        lastChapter = attributes.lastChapter?.toLongOrNull() ?: -1L,
        version = attributes.version,
        createdAt = attributes.createdAt.parseMangaDexTimeToDateTime(),
        updatedAt = attributes.updatedAt.parseMangaDexTimeToDateTime(),
        tagToId = tagToId,
        publicationDemographic = attributes.publicationDemographic,
        savedAtLocal = localDateTimeNow(),
        latestUploadedChapter = attributes.latestUploadedChapter,
        year = attributes.year ?: -1,
        authors = authors,
        artists = artists,
        offset = 0
    )
}

fun Manga.toRecentMangaResource(): RecentMangaResource {
    return RecentMangaResource(
        id = id,
        description = descriptionEnglish,
        coverArt = coverArtUrl(this),
        titleEnglish = titleEnglish,
        alternateTitles = alternateTitles,
        originalLanguage = attributes.originalLanguage,
        availableTranslatedLanguages = attributes.availableTranslatedLanguages
            .filterNotNull(),
        publicationDemographic = attributes.publicationDemographic,
        status = attributes.status,
        contentRating = attributes.contentRating,
        lastVolume = attributes.lastVolume?.toIntOrNull() ?: - 1,
        lastChapter = attributes.lastChapter?.toLongOrNull() ?: -1L,
        version = attributes.version,
        createdAt = attributes.createdAt.parseMangaDexTimeToDateTime(),
        updatedAt = attributes.updatedAt.parseMangaDexTimeToDateTime(),
        tagToId = tagToId,
        authors = authors,
        artists = artists,
        year = attributes.year ?: -1,
        latestUploadedChapter = attributes.latestUploadedChapter
    )
}

fun Manga.toSavedMangaEntity(saved: SavedMangaEntity? = null): SavedMangaEntity {
    val network = this
    return SavedMangaEntity(
        id = network.id,
        progressState = saved?.progressState ?: ProgressState.NotStarted,
        originalCoverArtUrl = coverArtUrl(network),
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
        bookmarked = saved?.bookmarked ?: false,
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
        coverArt = saved?.coverArt ?: ""
    )
}
