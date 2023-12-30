package io.silv.domain.manga.repository

import io.silv.domain.Syncable
import io.silv.domain.manga.model.Manga
import io.silv.domain.manga.model.MangaWithChapters
import kotlinx.coroutines.flow.Flow

interface MangaRepository: Syncable {

    suspend fun getMangaById(id: String): Manga?

    suspend fun saveManga(manga: Manga)

    suspend fun updateManga(manga: Manga)

    suspend fun saveManga(list: List<Manga>)

    fun observeMangaById(id: String): Flow<Manga?>

    fun observeLibraryManga(): Flow<List<Manga>>

    fun observeManga(): Flow<List<Manga>>

    fun observeLibraryMangaWithChapters(): Flow<List<MangaWithChapters>>
}