package io.silv.data.mappers

import io.silv.common.time.parseMangaDexTimeToDateTime
import io.silv.database.entity.chapter.ChapterEntity
import io.silv.network.model.chapter.ChapterDto

fun ChapterDto.toChapterEntity(prev: ChapterEntity? = null): ChapterEntity {
    val chapter = this
    return ChapterEntity(
        id = chapter.id,
        mangaId = chapter.relationships.find { it.type == "manga" }?.id
            ?: throw IllegalStateException("Chapter had no related manga id"),
        volume = chapter.attributes.volume?.toIntOrNull() ?: -1,
        title = chapter.attributes.title ?: "Vol: ${chapter.attributes.volume} Chap: ${chapter.attributes.chapter}",
        pages = chapter.attributes.pages,
        chapterNumber = chapter.attributes.chapter?.toDoubleOrNull() ?: -1.0,
        createdAt = chapter.attributes.createdAt.parseMangaDexTimeToDateTime(),
        updatedAt = chapter.attributes.updatedAt.parseMangaDexTimeToDateTime(),
        readableAt = chapter.attributes.readableAt.parseMangaDexTimeToDateTime(),
        uploader = chapter.attributes.uploader,
        url = chapter.attributes.externalUrl?.replace("\\", "") ?: "",
        languageCode = chapter.attributes.translatedLanguage ?: "",
        version = chapter.attributes.version,
        scanlator = chapter.relationships.find { it.type == "scanlation_group" }?.attributes?.name ?: "",
        scanlationGroupId = chapter.relationships.find { it.type == "scanlation_group" }?.id,
        user = chapter.relationships.find { it.type == "user" }?.attributes?.username,
        userId = chapter.relationships.find { it.type == "user" }?.id,
        bookmarked = prev?.bookmarked == true,
        lastPageRead = prev?.lastPageRead
    )
}