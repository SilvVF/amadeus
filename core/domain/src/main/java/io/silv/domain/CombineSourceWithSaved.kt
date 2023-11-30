package io.silv.domain

import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import io.silv.data.manga.SavedMangaRepository
import io.silv.database.entity.manga.SourceMangaResource
import io.silv.model.SavableManga
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class CombineSourceMangaWithSaved(
    private val saved: SavedMangaRepository,
) {

    operator fun invoke(
        pagingData: Flow<PagingData<SourceMangaResource>>,
        scope: CoroutineScope
    ): Flow<PagingData<SavableManga>> {
        return combine(
            saved.getSavedMangas(),
            pagingData.cachedIn(scope)
        ) { saved, resources ->
            resources.map { res ->
                SavableManga(res, saved.find { it.id == res.id })
            }
        }
    }
}