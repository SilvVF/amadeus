package io.silv.manga.domain

import io.silv.core.Mapper
import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.local.entity.MangaResource
import io.silv.manga.local.entity.ProgressState
import io.silv.manga.local.entity.SavedMangaEntity
import io.silv.manga.network.mangadex.models.chapter.Chapter
import io.silv.manga.network.mangadex.models.manga.Manga

typealias ChapterWithPrevEntity = Pair<Chapter, ChapterEntity?>

object ChapterToChapterEntityMapper: Mapper<ChapterWithPrevEntity, ChapterEntity> {
    override fun map(from: ChapterWithPrevEntity): ChapterEntity {
        val (chapter, prev) = from
        return ChapterEntity(
            id = chapter.id,
            mangaId = chapter.relationships.find { it.type == "manga" }?.id
                ?: throw IllegalStateException("Chapter had no related manga id"),
            progressState = prev?.progressState ?: ProgressState.NotStarted,
            volume = chapter.attributes.volume,
            title = chapter.attributes.title ?: "no title",
            pages = chapter.attributes.pages,
            chapterNumber = chapter.attributes.chapter?.toIntOrNull() ?: 0,
            chapterImages = prev?.chapterImages ?: emptyList(),
            createdAt = chapter.attributes.createdAt,
            updatedAt = chapter.attributes.updatedAt,
        )
    }
}

object MangaToMangaResourceMapper: Mapper<Pair<Manga, MangaResource?>, MangaResource> {

    override fun map(from: Pair<Manga, MangaResource?>): MangaResource {
        val (manga, resource) = from
        return with(manga) {
            MangaResource(
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
                lastVolume = attributes.lastVolume,
                lastChapter = attributes.lastChapter,
                version = attributes.version,
                createdAt = attributes.createdAt,
                updatedAt = attributes.updatedAt,
            )
        }
    }
}

object MangaEntityMapper: Mapper<Pair<Manga, SavedMangaEntity?>, SavedMangaEntity> {

    override fun map(from: Pair<Manga, SavedMangaEntity?>): SavedMangaEntity {
        val (network, saved) = from
        return SavedMangaEntity(
            id = network.id,
            readChapters = saved?.readChapters ?: emptyList(),
            chapterToLastReadPage = saved?.chapterToLastReadPage ?: emptyMap(),
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
            lastChapter = network.attributes.lastChapter,
            lastVolume = network.attributes.lastVolume,
            version = network.attributes.version,
            bookmarked = saved?.bookmarked ?: false,
            volumeToCoverArt = saved?.volumeToCoverArt ?: emptyMap(),
            createdAt = network.attributes.createdAt,
            updatedAt = network.attributes.updatedAt,
        )
    }
}