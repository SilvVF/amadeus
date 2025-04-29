package io.silv.data.manga.repository

import io.silv.data.manga.model.Manga
import kotlinx.coroutines.flow.StateFlow

interface TopYearlyFetcher {
    suspend fun getYearlyTopMangaByTagId(tagId: String, amount: Int = 20): List<StateFlow<Manga>>
}