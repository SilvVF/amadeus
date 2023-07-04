package io.silv.manga.domain.repositorys

import io.silv.manga.network.mangadex.models.ContentRating

data class SavedMangaQuery(
    val translatedLanguage: String = "en",
    val contentRating: List<ContentRating> = ContentRating.values().toList()
)
