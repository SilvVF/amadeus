package io.silv.manga.network.mangadex.models.manga

import android.os.Parcelable
import io.silv.manga.network.mangadex.models.ContentRating
import io.silv.manga.network.mangadex.models.LocalizedString
import io.silv.manga.network.mangadex.models.PublicationDemographic
import io.silv.manga.network.mangadex.models.State
import io.silv.manga.network.mangadex.models.Status
import io.silv.manga.network.mangadex.models.common.Tag
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