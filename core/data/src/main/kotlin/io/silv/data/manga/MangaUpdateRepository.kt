package io.silv.data.manga

import io.silv.database.entity.manga.SavedMangaEntity
import io.silv.database.entity.relations.MangaUpdateEntityWithManga
import io.silv.network.model.manga.Manga
import kotlinx.coroutines.flow.Flow

interface MangaUpdateRepository {

    fun observeAllUpdates(): Flow<List<MangaUpdateEntityWithManga>>

    suspend fun createUpdate(
        prev: SavedMangaEntity,
        new: Manga
    )
}
