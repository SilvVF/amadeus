package io.silv.manga.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.silv.manga.network.mangadex.models.ContentRating
import io.silv.manga.network.mangadex.models.Status
import io.silv.manga.sync.AmadeusEntity
import kotlinx.datetime.Clock

@Entity
data class SavedMangaEntity(

    @PrimaryKey override val id: String,

    val progressState: ProgressState = ProgressState.NotStarted,

    val coverArt: String,

    val description: String,

    val titleEnglish: String,

    val alternateTitles: Map<String, String>,

    val originalLanguage: String,

    val availableTranslatedLanguages: List<String>,

    val status: Status,

    val contentRating: ContentRating,

    val lastVolume: String? = null,

    val lastChapter: String? = null,

    val version: Int,

    val bookmarked: Boolean,

    val createdAt: String,

    val updatedAt: String,

    val chaptersIds: List<String>,

    val volumeToCoverArt: Map<String, String>,

    val savedLocalAtEpochSeconds: Long = Clock.System.now().epochSeconds
): AmadeusEntity {

    constructor(mangaResource: MangaResource): this(
        id = mangaResource.id,
        progressState = ProgressState.NotStarted,
        coverArt = mangaResource.coverArt,
        description = mangaResource.description,
        titleEnglish = mangaResource.titleEnglish,
        alternateTitles = mangaResource.alternateTitles,
        originalLanguage = mangaResource.originalLanguage,
        availableTranslatedLanguages = mangaResource.availableTranslatedLanguages,
        status = mangaResource.status,
        contentRating = mangaResource.contentRating,
        lastVolume = mangaResource.lastVolume,
        lastChapter = mangaResource.lastChapter,
        version = mangaResource.version,
        bookmarked = false,
        createdAt = mangaResource.createdAt,
        updatedAt = mangaResource.updatedAt,
        chaptersIds = emptyList(),
        volumeToCoverArt = emptyMap(),
    )
}
