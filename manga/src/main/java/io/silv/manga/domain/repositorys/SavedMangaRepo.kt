@file:OptIn(ExperimentalCoroutinesApi::class)

package io.silv.manga.domain.repositorys

import io.silv.manga.local.dao.SavedMangaDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

internal class SavedMangaRepo(
    private val mangaDao: SavedMangaDao
) {

    fun getSavedMangaIds(): Flow<List<String>> =
        mangaDao.getAllAsFlow()
            .mapLatest { list -> list.map { it.id } }
}