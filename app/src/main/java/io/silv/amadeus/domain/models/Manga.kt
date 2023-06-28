package io.silv.amadeus.domain.models

import io.silv.amadeus.network.mangadex.models.ContentRating
import io.silv.amadeus.network.mangadex.models.Status

data class Manga(
    val id: String,
    val title: String,
    val altTitle: String,
    val availableTranslatedLanguages: List<String>,
    val allTitles: Map<String, String>,
    val allDescriptions: Map<String, String>,
    val description: String,
    val imageUrl: String,
    val lastVolume: Int,
    val lastChapter: Int,
    val status: Status,
    val year: Int,
    val contentRating: ContentRating,
    val genres: List<String>
)