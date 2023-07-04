package io.silv.manga.domain.models

import android.os.Parcelable
import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.local.entity.ProgressState
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
data class DomainChapter(
    val id: String,
    val downloaded: Boolean,
    val progress: ProgressState = ProgressState.NotStarted,
    val mangaId: String,
    val imageUris: List<String> = emptyList(),
    val title: String? = null,
    val volume: String? = null,
    val chapter: String? = null,
    val pages: Int = 0,
    val translatedLanguage: String,
    val uploader: String,
    val externalUrl: String? = null,
    val version: Int,
    val createdAt: String,
    val updatedAt: String,
    val readableAt: String,
): Parcelable, Serializable {

    constructor(entity: ChapterEntity): this(
        id = entity.id,
        downloaded = false,
        progress = entity.progressState,
        mangaId = entity.mangaId,
        imageUris = emptyList(),
        title = entity.title,
        volume = entity.volume,
        chapter = entity.chapterNumber.toString(),
        pages = entity.pages,
        translatedLanguage = "en",
        uploader = "",
        externalUrl = null,
        version = 0,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        readableAt = ""
    )
}