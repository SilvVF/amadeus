@file:OptIn(ExperimentalCoroutinesApi::class)

package io.silv.manga.domain.repositorys

import io.silv.manga.local.entity.SavedMangaEntity
import io.silv.manga.sync.Syncable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

interface SavedMangaRepository: Syncable {

    suspend fun bookmarkManga(id: String)

    fun getSavedMangas(): Flow<List<SavedMangaEntity>>

    fun getSavedManga(id: String): Flow<SavedMangaEntity?>
}
