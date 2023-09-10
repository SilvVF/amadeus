package io.silv.manga.repositorys.manga

import io.silv.manga.local.entity.SavedMangaEntity
import io.silv.manga.local.entity.relations.MangaUpdateEntityWithManga
import io.silv.manga.network.mangadex.models.manga.Manga
import kotlinx.coroutines.flow.Flow

interface MangaUpdateRepository {

    fun observeAllUpdates(): Flow<List<MangaUpdateEntityWithManga>>

    suspend fun createUpdate(
        prev: SavedMangaEntity,
        new: Manga
    )
}
