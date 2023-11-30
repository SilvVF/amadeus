package io.silv.data.manga

import androidx.paging.PagingData
import io.silv.database.entity.manga.SourceMangaResource
import kotlinx.coroutines.flow.Flow

interface QuickSearchMangaRepository {

    fun pagingData(query: String): Flow<PagingData<SourceMangaResource>>
}


