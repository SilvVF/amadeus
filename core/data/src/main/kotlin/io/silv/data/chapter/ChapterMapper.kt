package io.silv.data.chapter

import io.silv.database.entity.chapter.ChapterEntity
import io.silv.domain.chapter.model.Chapter

object ChapterMapper {

    private val implementedImageSources =
        listOf(
            "mangaplus.shueisha",
            "azuki.co",
            "mangahot.jp",
            "bilibilicomics.com",
            "comikey.com",
        )

    fun mapChapter(
        entity: ChapterEntity
    ): Chapter =
        Chapter(
            id = entity.id,
            url = entity.url,
            bookmarked = entity.bookmarked,
            downloaded = false,
            mangaId = entity.mangaId,
            title = entity.title,
            volume = entity.volume,
            chapter = entity.chapterNumber,
            pages = entity.pages,
            translatedLanguage = entity.languageCode,
            uploader = entity.uploader ?: "",
            lastReadPage = entity.lastPageRead,
            version = entity.version,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            readableAt = entity.readableAt,
            scanlationGroupToId =
            entity
                .scanlator.let { group ->
                    entity.scanlationGroupId?.let { id ->
                        group to id
                    }
                },
            userToId =
            entity
                .user?.let { user ->
                    entity.userId?.let { id ->
                        user to id
                    }
                },
            ableToDownload = implementedImageSources.any { it in (entity.url) } || entity.url.isBlank(),
        )

    fun toEntity(
       chapter: Chapter
    ): ChapterEntity {
        with(chapter) {
            return ChapterEntity(
                id = id,
                mangaId = mangaId,
                scanlator = scanlationGroupToId?.first ?: "",
                url = url,
                title = title,
                languageCode = translatedLanguage,
                scanlationGroupId = scanlationGroupToId?.second,
                userId = userToId?.second,
                user = userToId?.first,
                volume = volume,
                lastPageRead = lastReadPage,
                pages = pages,
                bookmarked = bookmarked,
                chapterNumber = chapter.chapter,
                createdAt = createdAt,
                updatedAt = updatedAt,
                readableAt = readableAt,
                uploader = uploader,
                version = version,
            )
        }
    }
}