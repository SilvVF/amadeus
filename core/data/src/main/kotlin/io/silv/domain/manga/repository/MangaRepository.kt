package io.silv.domain.manga.repository

import io.silv.domain.manga.model.Manga
import io.silv.domain.manga.model.MangaUpdate
import io.silv.domain.manga.model.MangaWithChapters
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

interface MangaRepository {

    suspend fun getMangaById(id: String): Manga?

    suspend fun updateManga(list: List<Manga>)

    suspend fun updateManga(manga: Manga)

    suspend fun getMangaByTitle(title: String): Manga?

    suspend fun upsertManga(update: MangaUpdate)

    suspend fun upsertManga(updates: List<MangaUpdate>, withTransaction: Boolean = true)

    fun observeMangaById(id: String): Flow<Manga?>

    fun observeLibraryManga(): Flow<List<Manga>>

    suspend fun getLibraryManga(): List<Manga>

    fun observeManga(): Flow<List<Manga>>

    fun observeLibraryMangaWithChapters(): Flow<List<MangaWithChapters>>

    suspend fun getLibraryMangaWithChapters(): List<MangaWithChapters>

    fun observeMangaWithChaptersById(id: String): Flow<MangaWithChapters>

    suspend fun getMangaWithChaptersById(id: String): MangaWithChapters?

    suspend fun deleteUnused()

    fun observeUnusedCount(): Flow<Int>

    fun observeLastLibrarySynced(): Flow<LocalDateTime?>
}