package io.silv.domain.manga.repository

import io.silv.domain.Syncable
import io.silv.domain.manga.model.Manga
import io.silv.domain.manga.model.MangaUpdate
import io.silv.domain.manga.model.MangaWithChapters
import kotlinx.coroutines.flow.Flow

interface MangaRepository: Syncable {

    suspend fun getMangaById(id: String): Manga?

    suspend fun updateManga(list: List<Manga>)

    suspend fun updateManga(manga: Manga)

    suspend fun upsertManga(update: MangaUpdate)

    suspend fun upsertManga(updates: List<MangaUpdate>, withTransaction: Boolean = true)

    fun observeMangaById(id: String): Flow<Manga?>

    fun observeLibraryManga(): Flow<List<Manga>>

    fun observeManga(): Flow<List<Manga>>

    fun observeLibraryMangaWithChapters(): Flow<List<MangaWithChapters>>
}