package io.silv.domain.manga.repository

import io.silv.domain.manga.model.MangaUpdateWithManga
import kotlinx.coroutines.flow.Flow

interface MangaUpdateRepository {

    fun observeAllUpdates(): Flow<List<MangaUpdateWithManga>>

    suspend fun createUpdates(mangaIds: List<String>)
}
