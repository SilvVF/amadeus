package io.silv.data.manga

import io.silv.common.model.ProgressState
import io.silv.common.model.ReadingStatus
import io.silv.common.time.localDateTimeNow
import io.silv.common.time.parseMangaDexTimeToDateTime
import io.silv.data.chapter.ChapterMapper
import io.silv.data.mappers.alternateTitles
import io.silv.data.mappers.artists
import io.silv.data.mappers.authors
import io.silv.data.mappers.coverArtUrl
import io.silv.data.mappers.descriptionEnglish
import io.silv.data.mappers.tagToId
import io.silv.data.mappers.titleEnglish
import io.silv.database.entity.manga.MangaEntity
import io.silv.database.entity.manga.MangaEntityWithChapters
import io.silv.data.manga.model.Manga
import io.silv.data.manga.model.MangaUpdate
import io.silv.data.manga.model.MangaWithChapters
import io.silv.network.model.manga.MangaDto

object MangaMapper {

    fun dtoToManga(
        mangaDto: MangaDto
    ): Manga = with(mangaDto) {
        Manga(
            id = id,
            inLibrary = false,
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
            year = attributes.year ?: -1,
            latestUploadedChapter = attributes.latestUploadedChapter,
            authors = authors,
            artists = artists,
            progressState = ProgressState.NotStarted,
            readingStatus = ReadingStatus.None,
            coverLastModified = -1L,
            savedAtLocal = localDateTimeNow(),
            lastSyncedForUpdates = null
        )
    }

    fun mangaToUpdate(manga: Manga) : MangaUpdate {
        return with(manga) {
             MangaUpdate(
                id = id,
                favorite = inLibrary,
                description = description,
                progressState = progressState,
                coverArt = coverArt,
                title = titleEnglish,
                alternateTitles = alternateTitles,
                originalLanguage = originalLanguage,
                availableTranslatedLanguages = availableTranslatedLanguages,
                status = status,
                tagToId = tagToId,
                contentRating = contentRating,
                lastVolume = lastVolume,
                lastChapter = lastChapter,
                version = version,
                createdAt = createdAt,
                updatedAt = updatedAt,
                publicationDemographic = publicationDemographic,
                readingStatus = readingStatus,
                year = year,
                artists = artists,
                authors = authors,
                latestUploadedChapter = latestUploadedChapter
            )
        }
    }

    fun toEntity(manga: Manga) : MangaEntity {
        return with(manga) {
             MangaEntity(
                id = id,
                favorite = inLibrary,
                description = description,
                progressState = progressState,
                coverArt = coverArt,
                title = titleEnglish,
                alternateTitles = alternateTitles,
                originalLanguage = originalLanguage,
                availableTranslatedLanguages = availableTranslatedLanguages,
                status = status,
                tagToId = tagToId,
                contentRating = contentRating,
                lastVolume = lastVolume,
                lastChapter = lastChapter,
                version = version,
                createdAt = createdAt,
                updatedAt = updatedAt,
                savedAtLocal = savedAtLocal,
                publicationDemographic = publicationDemographic,
                readingStatus = readingStatus,
                year = year,
                artists = artists,
                authors = authors,
                latestUploadedChapter = latestUploadedChapter,
                coverLastModified = manga.coverLastModified,
                lastSyncedForUpdates = manga.lastSyncedForUpdates
            )
        }
    }


    fun mapManga(
        entity: MangaEntity
    ): Manga {
        return Manga(
            id = entity.id,
            inLibrary = entity.favorite,
            description = entity.description,
            progressState = entity.progressState,
            coverArt = entity.coverArt,
            titleEnglish = entity.title,
            alternateTitles = entity.alternateTitles,
            originalLanguage = entity.originalLanguage,
            availableTranslatedLanguages = entity.availableTranslatedLanguages,
            status = entity.status,
            tagToId = entity.tagToId,
            contentRating = entity.contentRating,
            lastVolume = entity.lastVolume,
            lastChapter = entity.lastChapter,
            version = entity.version,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            savedAtLocal = entity.savedAtLocal,
            publicationDemographic = entity.publicationDemographic,
            readingStatus = entity.readingStatus,
            year = entity.year,
            artists = entity.artists,
            authors = entity.authors,
            latestUploadedChapter = entity.latestUploadedChapter,
            coverLastModified = entity.coverLastModified,
            lastSyncedForUpdates = entity.lastSyncedForUpdates
        )
    }

    fun mapMangaWithChapters(
        entity: MangaEntityWithChapters
    ): MangaWithChapters {
        return MangaWithChapters(
            manga = entity.manga.let(MangaMapper::mapManga),
            chapters = entity.chapters.map(ChapterMapper::mapChapter)
        )
    }
}
