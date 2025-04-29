package io.silv.data.manga.repository

import io.silv.data.manga.model.Manga
import io.silv.data.manga.model.MangaUpdate
import io.silv.data.manga.model.MangaWithChapters
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

interface MangaRepository {

    suspend fun getMangaById(id: String): Manga?

    suspend fun updateManga(update:  MangaUpdate)

    suspend fun getMangaByTitle(title: String): Manga?

    suspend fun insertManga(manga: List<Manga>, withTransaction: Boolean = true)

    suspend fun insertManga(manga: Manga)

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