package io.silv.domain.manga.repository

import io.silv.domain.manga.model.Manga
import kotlinx.coroutines.flow.StateFlow

interface TopYearlyFetcher {
    suspend fun getYearlyTopMangaByTagId(tagId: String, amount: Int = 20): List<StateFlow<Manga>>
}