package io.silv.data.mappers

import io.silv.common.model.ProgressState
import io.silv.common.time.parseMangaDexTimeToDateTime
import io.silv.database.entity.chapter.ChapterEntity
import io.silv.network.model.chapter.Chapter

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