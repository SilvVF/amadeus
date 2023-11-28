package io.silv.network.model.manga

import android.os.Parcelable
import io.silv.common.model.ContentRating
import io.silv.network.model.LocalizedString
import io.silv.common.model.PublicationDemographic
import io.silv.common.model.State
import io.silv.common.model.Status
import io.silv.network.model.common.Tag
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class MangaAttributes(
    val title: LocalizedString,
    val altTitles: List<LocalizedString>,
    val description: LocalizedString,
    val isLocked: Boolean,
    val links: Map<String, String>? = emptyMap(),
    val originalLanguage: String,
    val lastVolume: String? = null,
    val lastChapter: String? = null,
    val publicationDemographic: PublicationDemographic? = null,
    val status: Status,
    val year: Int? = null,
    val contentRating: ContentRating,
    val chapterNumbersResetOnNewVolume: Boolean,
    val availableTranslatedLanguages: List<String?> = emptyList(),
    val latestUploadedChapter: String? = null,
    val tags: List<Tag>,
    val state: State,
    val version: Int,
    val createdAt: String,
    val updatedAt: String
): Parcelable