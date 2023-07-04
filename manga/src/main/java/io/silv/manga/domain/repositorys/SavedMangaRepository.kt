@file:OptIn(ExperimentalCoroutinesApi::class)

package io.silv.manga.domain.repositorys

import io.silv.manga.local.dao.SavedMangaDao
import io.silv.manga.local.entity.MangaResource
import io.silv.manga.local.entity.SavedMangaEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

interface SavedMangaRepository {

    suspend fun bookmarkManga(id: String)

    fun getSavedManga(): Flow<List<SavedMangaEntity>>
}
