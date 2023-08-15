package io.silv.manga.domain

import io.silv.core.Mapper
import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.local.entity.FilteredMangaResource
import io.silv.manga.local.entity.FilteredMangaYearlyResource
import io.silv.manga.local.entity.PopularMangaResource
import io.silv.manga.local.entity.ProgressState
import io.silv.manga.local.entity.QuickSearchMangaResource
import io.silv.manga.local.entity.ReadingStatus
import io.silv.manga.local.entity.RecentMangaResource
import io.silv.manga.local.entity.SavedMangaEntity
import io.silv.manga.local.entity.SearchMangaResource
import io.silv.manga.local.entity.SeasonalMangaResource
import io.silv.manga.network.mangadex.models.chapter.Chapter
import io.silv.manga.network.mangadex.models.manga.Manga
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlin.time.Duration

typealias ChapterWithPrevEntity = Pair<Chapter, ChapterEntity?>

fun String.parseMangaDexTimeToDateTime(): LocalDateTime {
   // "2021-10-10T23:19:03+00:00",
    val text = this.replaceAfter('+', "").dropLast(1)
    return LocalDateTime.parse(text)
}

infix fun  LocalDateTime.minus(localDateTime: LocalDateTime): Duration {
    return this.toInstant(timeZone())
        .minus(
            localDateTime.toInstant(timeZone())
        )
}


object ChapterToChapterEntityMapper: Mapper<ChapterWithPrevEntity, ChapterEntity> {
    override fun map(from: ChapterWithPrevEntity): ChapterEntity {
        val (chapter, prev) = from
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
}

object MangaToSeasonalMangaResourceMapper: Mapper<Pair<Manga, SeasonalMangaResource?>, SeasonalMangaResource> {

    override fun map(from: Pair<Manga, SeasonalMangaResource?>): SeasonalMangaResource {
        val (manga, _) = from
        return with(manga) {
            SeasonalMangaResource(
                id = id,
                description = manga.descriptionEnglish,
                coverArt = coverArtUrl(manga),
                titleEnglish = manga.titleEnglish,
                alternateTitles = manga.alternateTitles,
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
                publicationDemographic = manga.attributes.publicationDemographic,
                latestUploadedChapter = attributes.latestUploadedChapter,
                year = attributes.year ?: -1,
                authors = authors,
                artists = artists
            )
        }
    }
}

object MangaToPopularMangaResourceMapper: Mapper<Pair<Manga, PopularMangaResource?>,PopularMangaResource> {

    override fun map(from: Pair<Manga, PopularMangaResource?>): PopularMangaResource {
        val (manga, _) = from
        return with(manga) {
            PopularMangaResource(
                id = id,
                description = manga.descriptionEnglish,
                coverArt = coverArtUrl(manga),
                titleEnglish = manga.titleEnglish,
                alternateTitles = manga.alternateTitles,
                originalLanguage = attributes.originalLanguage,
                availableTranslatedLanguages = attributes.availableTranslatedLanguages
                    .filterNotNull(),
                status = attributes.status,
                contentRating = attributes.contentRating,
                lastVolume = attributes.lastVolume?.toIntOrNull() ?: - 1,
                lastChapter = attributes.lastChapter?.toLongOrNull() ?: -1L,
                version = attributes.version,
                createdAt = attributes.createdAt.parseMangaDexTimeToDateTime(),
                updatedAt = attributes.updatedAt.parseMangaDexTimeToDateTime(),
                tagToId = tagToId,
                publicationDemographic = manga.attributes.publicationDemographic,
                latestUploadedChapter = attributes.latestUploadedChapter,
                year = attributes.year ?: -1,
                authors = authors,
                artists = artists
            )
        }
    }
}


object MangaToQuickSearchMangaResourceMapper: Mapper<Pair<Manga, QuickSearchMangaResource?>, QuickSearchMangaResource> {

    override fun map(from: Pair<Manga, QuickSearchMangaResource?>): QuickSearchMangaResource {
        val (manga, _) = from
        return with(manga) {
            QuickSearchMangaResource(
                id = id,
                description = manga.descriptionEnglish,
                coverArt = coverArtUrl(manga),
                titleEnglish = manga.titleEnglish,
                alternateTitles = manga.alternateTitles,
                originalLanguage = attributes.originalLanguage,
                availableTranslatedLanguages = attributes.availableTranslatedLanguages
                    .filterNotNull(),
                status = attributes.status,
                contentRating = attributes.contentRating,
                lastVolume = attributes.lastVolume?.toIntOrNull() ?: - 1,
                lastChapter = attributes.lastChapter?.toLongOrNull() ?: -1L,
                version = attributes.version,
                createdAt = attributes.createdAt.parseMangaDexTimeToDateTime(),
                updatedAt = attributes.updatedAt.parseMangaDexTimeToDateTime(),
                tagToId = tagToId,
                publicationDemographic = manga.attributes.publicationDemographic,
                latestUploadedChapter = attributes.latestUploadedChapter,
                year = attributes.year ?: -1,
                authors = authors,
                artists = artists
            )
        }
    }
}

object MangaToSearchMangaResourceMapper: Mapper<Pair<Manga, SearchMangaResource?>,SearchMangaResource> {

    override fun map(from: Pair<Manga, SearchMangaResource?>): SearchMangaResource {
        val (manga, _) = from
        return with(manga) {
            SearchMangaResource(
                id = id,
                description = manga.descriptionEnglish,
                coverArt = coverArtUrl(manga),
                titleEnglish = manga.titleEnglish,
                alternateTitles = manga.alternateTitles,
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
                publicationDemographic = manga.attributes.publicationDemographic,
                latestUploadedChapter = attributes.latestUploadedChapter,
                year = attributes.year ?: -1,
                authors = authors,
                artists = artists
            )
        }
    }
}


object MangaToFilteredYearlyMangaResourceMapper: Mapper<Pair<Manga, FilteredMangaYearlyResource?>, FilteredMangaYearlyResource> {

    override fun map(from: Pair<Manga, FilteredMangaYearlyResource?>): FilteredMangaYearlyResource {
        val (manga, saved) = from
        return with(manga) {
            FilteredMangaYearlyResource(
                id = id,
                description = manga.descriptionEnglish,
                coverArt = coverArtUrl(manga),
                titleEnglish = manga.titleEnglish,
                alternateTitles = manga.alternateTitles,
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
                topTags = saved?.topTags ?: emptyList(),
                topTagPlacement = saved?.topTagPlacement ?: emptyMap(),
                publicationDemographic = manga.attributes.publicationDemographic,
                latestUploadedChapter = attributes.latestUploadedChapter,
                year = attributes.year ?: -1,
                authors = authors,
                artists = artists
            )
        }
    }
}

object MangaToFilteredMangaResourceMapper: Mapper<Pair<Manga, FilteredMangaResource?>, FilteredMangaResource> {

    override fun map(from: Pair<Manga, FilteredMangaResource?>): FilteredMangaResource {
        val (manga, saved) = from
        return with(manga) {
            FilteredMangaResource(
                id = id,
                description = manga.descriptionEnglish,
                coverArt = coverArtUrl(manga),
                titleEnglish = manga.titleEnglish,
                alternateTitles = manga.alternateTitles,
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
                publicationDemographic = manga.attributes.publicationDemographic,
                savedAtLocal = timeNow(),
                latestUploadedChapter = attributes.latestUploadedChapter,
                year = attributes.year ?: -1,
                authors = authors,
                artists = artists
            )
        }
    }
}


object MangaToRecentMangaResourceMapper: Mapper<Pair<Manga, RecentMangaResource?>, RecentMangaResource> {

    override fun map(from: Pair<Manga, RecentMangaResource?>): RecentMangaResource {
        val (manga, _) = from
        return with(manga) {
            RecentMangaResource(
                id = id,
                description = manga.descriptionEnglish,
                coverArt = coverArtUrl(manga),
                titleEnglish = manga.titleEnglish,
                alternateTitles = manga.alternateTitles,
                originalLanguage = attributes.originalLanguage,
                availableTranslatedLanguages = attributes.availableTranslatedLanguages
                    .filterNotNull(),
                publicationDemographic = manga.attributes.publicationDemographic,
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
    }
}


object MangaEntityMapper: Mapper<Pair<Manga, SavedMangaEntity?>, SavedMangaEntity> {

    override fun map(from: Pair<Manga, SavedMangaEntity?>): SavedMangaEntity {
        val (network, saved) = from
        return SavedMangaEntity(
            id = network.id,
            progressState = saved?.progressState ?: ProgressState.NotStarted,
            coverArt = coverArtUrl(network),
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
            latestUploadedChapter = network.attributes.latestUploadedChapter
        )
    }
}