package io.silv.amadeus.network.models.manga

import io.silv.amadeus.network.models.ContentRating
import io.silv.amadeus.network.models.PublicationDemographic
import io.silv.amadeus.network.models.State
import io.silv.amadeus.network.models.Status
import io.silv.amadeus.network.models.common.LocalizedString
import io.silv.amadeus.network.models.common.Tag
import kotlinx.serialization.Serializable

@Serializable
data class MangaAttributes(
    val title: LocalizedString,
    val altTitles: List<LocalizedString>,
    val description: LocalizedString,
    val isLocked: Boolean,
    val links: Map<String, String>,
    val originalLanguage: String,
    val lastVolume: String? = null,
    val lastChapter: String? = null,
    val publicationDemographic: PublicationDemographic? = null,
    val status: Status,
    val year: Int? = null,
    val contentRating: ContentRating,
    val chapterNumbersResetOnNewVolume: Boolean,
    val availableTranslatedLanguages: List<String>,
    val latestUploadedChapter: String,
    val tags: List<Tag>,
    val state: State,
    val version: Int,
    val createdAt: String,
    val updatedAt: String
)