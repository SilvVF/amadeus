package io.silv.manga.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.silv.manga.network.mangadex.models.ContentRating
import io.silv.manga.network.mangadex.models.Status
import kotlinx.datetime.Clock

@Entity
data class SavedMangaEntity(

    @PrimaryKey override val id: String,

    val progressState: ProgressState = ProgressState.NotStarted,

    val readChapters: List<String>,

    val chapterToLastReadPage: Map<String, Int>,

    val coverArt: String,

    val description: String,

    val titleEnglish: String,

    val alternateTitles: Map<String, String>,

    val originalLanguage: String,

    val availableTranslatedLanguages: List<String>,

    val status: Status,

    val tagToId: Map<String, String>,

    val contentRating: ContentRating,

    val lastVolume: String? = null,

    val lastChapter: String? = null,

    val version: Int,

    val bookmarked: Boolean,

    val createdAt: String,

    val updatedAt: String,

    val volumeToCoverArt: Map<String, String>,

    val savedLocalAtEpochSeconds: Long = Clock.System.now().epochSeconds
): AmadeusEntity<Any?> {
    constructor(mangaResource: MangaResource): this(
        id = mangaResource.id,
        progressState = ProgressState.NotStarted,
        chapterToLastReadPage = emptyMap(),
        readChapters = emptyList(),
        coverArt = mangaResource.coverArt,
        description = mangaResource.description,
        titleEnglish = mangaResource.titleEnglish,
        alternateTitles = mangaResource.alternateTitles,
        originalLanguage = mangaResource.originalLanguage,
        availableTranslatedLanguages = mangaResource.availableTranslatedLanguages,
        status = mangaResource.status,
        tagToId = mangaResource.tagToId,
        contentRating = mangaResource.contentRating,
        lastVolume = mangaResource.lastVolume,
        lastChapter = mangaResource.lastChapter,
        version = mangaResource.version,
        bookmarked = false,
        createdAt = mangaResource.createdAt,
        updatedAt = mangaResource.updatedAt,
        volumeToCoverArt = emptyMap(),
    )
}
