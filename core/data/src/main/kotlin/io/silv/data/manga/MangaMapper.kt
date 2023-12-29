package io.silv.data.manga

import io.silv.data.chapter.ChapterMapper
import io.silv.database.entity.manga.MangaEntity
import io.silv.database.entity.relations.MangaEntityWithChapters
import io.silv.database.entity.relations.MangaUpdateEntityWithManga
import io.silv.domain.manga.model.Manga
import io.silv.domain.manga.model.MangaUpdateWithManga
import io.silv.domain.manga.model.MangaWithChapters
import kotlinx.collections.immutable.toPersistentList

object MangaMapper {

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
            savedLocalAtEpochSeconds = entity.savedAtLocal,
            publicationDemographic = entity.publicationDemographic,
            readingStatus = entity.readingStatus,
            year = entity.year,
            artists = entity.artists,
            authors = entity.authors,
            latestUploadedChapter = entity.latestUploadedChapter
        )
    }

    fun toEntity(manga: Manga) : MangaEntity {
        with(manga) {
            return MangaEntity(
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
                savedAtLocal = savedLocalAtEpochSeconds,
                publicationDemographic = publicationDemographic,
                readingStatus = readingStatus,
                year = year,
                artists = artists,
                authors = authors,
                latestUploadedChapter = latestUploadedChapter
            )
        }
    }

    fun mapUpdateWithManga(mangaUpdateEntityWithManga: MangaUpdateEntityWithManga) =
        MangaUpdateWithManga(
            id = mangaUpdateEntityWithManga.update.id,
            savedMangaId = mangaUpdateEntityWithManga.update.savedMangaId,
            updateType = mangaUpdateEntityWithManga.update.updateType,
            createdAt = mangaUpdateEntityWithManga.update.createdAt,
            manga = mangaUpdateEntityWithManga.manga.let(::mapManga)
        )

    fun mapMangaWithChapters(
        entity: MangaEntityWithChapters
    ): MangaWithChapters {
        return MangaWithChapters(
            manga = entity.manga.let(MangaMapper::mapManga),
            chapters = entity.chapters.map(ChapterMapper::mapChapter).toPersistentList()
        )
    }
}
