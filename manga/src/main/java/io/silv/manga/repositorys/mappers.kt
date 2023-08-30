package io.silv.manga.repositorys

import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.local.entity.ProgressState
import io.silv.manga.local.entity.ReadingStatus
import io.silv.manga.local.entity.SavedMangaEntity
import io.silv.manga.local.entity.manga_resource.FilteredMangaResource
import io.silv.manga.local.entity.manga_resource.FilteredMangaYearlyResource
import io.silv.manga.local.entity.manga_resource.PopularMangaResource
import io.silv.manga.local.entity.manga_resource.QuickSearchMangaResource
import io.silv.manga.local.entity.manga_resource.RecentMangaResource
import io.silv.manga.local.entity.manga_resource.SearchMangaResource
import io.silv.manga.local.entity.manga_resource.SeasonalMangaResource
import io.silv.manga.local.entity.manga_resource.TempMangaResource
import io.silv.manga.network.mangadex.models.chapter.Chapter
import io.silv.manga.network.mangadex.models.manga.Manga

fun Chapter.toChapterEntity(prev: ChapterEntity? = null): ChapterEntity {
    val chapter = this
    return ChapterEntity(
        id = chapter.id,
        mangaId = chapter.relationships.find { it.type == "manga" }?.id
            ?: throw IllegalStateException("Chapter had no related manga id"),
        progressState = prev?.progressState ?: ProgressState.NotStarted,
        volume = chapter.attributes.volume?.toIntOrNull() ?: -1,
        title = chapter.attributes.title ?: "",
        pages = chapter.attributes.pages,
        chapterNumber = chapter.attributes.chapter?.toLongOrNull() ?: -1L,
        chapterImages = prev?.chapterImages ?: emptyList(),
        createdAt = chapter.attributes.createdAt.parseMangaDexTimeToDateTime(),
        updatedAt = chapter.attributes.updatedAt.parseMangaDexTimeToDateTime(),
        readableAt = chapter.attributes.readableAt.parseMangaDexTimeToDateTime(),
        uploader = chapter.attributes.uploader,
        externalUrl = chapter.attributes.externalUrl,
        languageCode = chapter.attributes.translatedLanguage ?: "",
        version = chapter.attributes.version,
        scanlationGroup = chapter.relationships.find { it.type == "scanlation_group" }?.attributes?.name,
        scanlationGroupId = chapter.relationships.find { it.type == "scanlation_group" }?.id,
        user = chapter.relationships.find { it.type == "user" }?.attributes?.username,
        userId = chapter.relationships.find { it.type == "user" }?.id,
        bookmarked = prev?.bookmarked ?: false,
        lastPageRead = prev?.lastPageRead ?: 0
    )
}

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
        savedAtLocal = timeNow(),
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
        savedAtLocal = timeNow(),
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
