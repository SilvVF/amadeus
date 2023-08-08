package io.silv.manga.domain.models

import android.os.Parcelable
import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.local.entity.ProgressState
import kotlinx.datetime.LocalDateTime
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import java.io.Serializable

@Parcelize
data class SavableChapter(
    val id: String,
    val downloaded: Boolean,
    val progress: ProgressState,
    val mangaId: String,
    val imageUris: List<String> = emptyList(),
    val title: String,
    val volume: Int,
    val chapter: Long,
    val pages: Int = 0,
    val translatedLanguage: String,
    val uploader: String,
    val externalUrl: String? = null,
    val scanlationGroupToId: Pair<String, String>? = null,
    val userToId: Pair<String,String>? = null,
    val version: Int,
    @TypeParceler<LocalDateTime, LocalDateTimeParceler>
    val createdAt: LocalDateTime,
    @TypeParceler<LocalDateTime, LocalDateTimeParceler>
    val updatedAt: LocalDateTime,
    @TypeParceler<LocalDateTime, LocalDateTimeParceler>
    val readableAt: LocalDateTime,
    val ableToDownload: Boolean,
): Parcelable, Serializable {

    constructor(entity: ChapterEntity): this(
        id = entity.id,
        downloaded = entity.chapterImages.isNotEmpty(),
        progress = entity.progressState,
        mangaId = entity.mangaId,
        imageUris = entity.chapterImages,
        title = entity.title,
        volume = entity.volume,
        chapter = entity.chapterNumber,
        pages = entity.pages,
        translatedLanguage = entity.languageCode,
        uploader = entity.uploader ?: "",
        externalUrl = entity.externalUrl,
        version = entity.version,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        readableAt = entity.readableAt,
        scanlationGroupToId = if (entity.scanlationGroup != null && entity.scanlationGroupId != null) {
            entity.scanlationGroup to entity.scanlationGroupId
        } else null,
        userToId = if (entity.user != null && entity.userId != null) { entity.user to entity.userId } else null,
        ableToDownload = entity.externalUrl == null || implementedImageSources.any { it in entity.externalUrl },
    )


    companion object {
        val implementedImageSources = listOf(
            "mangaplus.shueisha",
            "azuki.co",
            "mangahot.jp",
            "bilibilicomics.com",
            "comikey.com"
        )
    }
}