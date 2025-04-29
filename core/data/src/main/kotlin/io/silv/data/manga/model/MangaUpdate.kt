package io.silv.data.manga.model

import io.silv.common.model.ContentRating
import io.silv.common.model.ProgressState
import io.silv.common.model.PublicationDemographic
import io.silv.common.model.ReadingStatus
import io.silv.common.model.Status
import kotlinx.datetime.LocalDateTime

data class MangaUpdate(
    val id: String,
    val coverArt: String? = null,
    val title: String? = null,
    val version: Int? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val favorite: Boolean? = null,
    val description: String? = null,
    val alternateTitles: Map<String, String>? = null,
    val originalLanguage: String? = null,
    val availableTranslatedLanguages: List<String>? = null,
    val status: Status? = null,
    val tagToId: Map<String, String>? = null,
    val contentRating: ContentRating? = null,
    val lastVolume: Int? = null,
    val lastChapter: Long? = null,
    val publicationDemographic: PublicationDemographic? = null,
    val year: Int? = null,
    val latestUploadedChapter: String? = null,
    val authors: List<String>? = null,
    val artists: List<String>? = null,
    val progressState: ProgressState? = null,
    val readingStatus: ReadingStatus? = null,
    val coverLastModified: Long? = null
)

fun Manga.toUpdate(): MangaUpdate {
    return MangaUpdate(
        id = id,
        coverArt = coverArt,
        title = titleEnglish,
        version = version,
        createdAt= createdAt,
        updatedAt = updatedAt,
        favorite = inLibrary,
        description = description, alternateTitles = alternateTitles,
        originalLanguage = originalLanguage,
        availableTranslatedLanguages = availableTranslatedLanguages,
        status = status,
        tagToId = tagToId,
        contentRating = contentRating,
        lastVolume = lastVolume,
        lastChapter = lastChapter,
        publicationDemographic = publicationDemographic,
        year = year,
        latestUploadedChapter = latestUploadedChapter,
        authors = authors,
        artists = artists,
        progressState = progressState,
        readingStatus = readingStatus,
        coverLastModified = coverLastModified
    )
}