package io.silv.manga.domain.models

import android.os.Parcelable
import io.silv.manga.local.entity.ProgressState
import io.silv.manga.network.mangadex.models.ContentRating
import io.silv.manga.network.mangadex.models.Status
import kotlinx.datetime.Clock
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
data class DomainSavedManga(
    val id: String,
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
    val bookmarked: Boolean,
    val volumesIds: List<String>,
    val createdAt: String,
    val updatedAt: String,
    val chaptersIds: List<String>,
    val volumeToCoverArt: Map<String, String>,
    val savedLocalAtEpochSeconds: Long = Clock.System.now().epochSeconds
): Parcelable, Serializable

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
    val savedLocalAtEpochSeconds: Long = Clock.System.now().epochSeconds
): Parcelable, Serializable