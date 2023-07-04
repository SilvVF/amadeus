package io.silv.manga.domain.models

import android.os.Parcelable
import io.silv.manga.local.entity.MangaResource
import io.silv.manga.local.entity.ProgressState
import io.silv.manga.local.entity.SavedMangaEntity
import io.silv.manga.network.mangadex.models.ContentRating
import io.silv.manga.network.mangadex.models.Status
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
data class DomainManga(
    val id: String,
    val bookmarked: Boolean = false,
    val description: String,
    val progressState: ProgressState = ProgressState.NotStarted,
    val coverArt: String,
    val titleEnglish: String,
    val alternateTitles: Map<String, String>,
    val originalLanguage: String,
    val availableTranslatedLanguages: List<String>,
    val status: Status,
    val contentRating: ContentRating,
    val lastVolume: String? = null,
    val lastChapter: String? = null,
    val version: Int,
    val createdAt: String,
    val updatedAt: String,
    val savedLocalAtEpochSeconds: Long = Clock.System.now().epochSeconds,
    val volumeToCoverArtUrl: Map<String, String>
): Parcelable, Serializable {
    constructor(savedManga: SavedMangaEntity) : this(
        id = savedManga.id,
        bookmarked = savedManga.bookmarked,
        description = savedManga.description,
        progressState = savedManga.progressState ,
        coverArt = savedManga.coverArt,
        titleEnglish = savedManga.titleEnglish,
        alternateTitles = savedManga.alternateTitles,
        originalLanguage = savedManga.originalLanguage,
        availableTranslatedLanguages = savedManga.availableTranslatedLanguages,
        status = savedManga.status,
        contentRating = savedManga.contentRating,
        lastVolume = savedManga.lastVolume,
        lastChapter = savedManga.lastChapter,
        version = savedManga.version,
        createdAt = savedManga.createdAt,
        updatedAt = savedManga.updatedAt,
        savedLocalAtEpochSeconds = savedManga.savedLocalAtEpochSeconds,
        volumeToCoverArtUrl = savedManga.volumeToCoverArt
    )
    constructor(mangaResource: MangaResource, savedManga: SavedMangaEntity?) : this(
        id = mangaResource.id,
        bookmarked = savedManga?.bookmarked ?: false,
        description = mangaResource.description,
        progressState = savedManga?.progressState ?: ProgressState.NotStarted,
        coverArt = mangaResource.coverArt,
        titleEnglish = mangaResource.titleEnglish,
        alternateTitles = mangaResource.alternateTitles,
        originalLanguage = mangaResource.originalLanguage,
        availableTranslatedLanguages = mangaResource.availableTranslatedLanguages,
        status = mangaResource.status,
        contentRating = mangaResource.contentRating,
        lastVolume = mangaResource.lastVolume,
        lastChapter = mangaResource.lastChapter,
        version = mangaResource.version,
        createdAt = mangaResource.createdAt,
        updatedAt = mangaResource.updatedAt,
        savedLocalAtEpochSeconds = mangaResource.savedLocalAtEpochSeconds,
        volumeToCoverArtUrl = savedManga?.volumeToCoverArt ?: emptyMap()
    )
}

fun MangaResource.mapToDomainManga(savedManga: SavedMangaEntity?): DomainManga {
    return DomainManga(this, savedManga)
}