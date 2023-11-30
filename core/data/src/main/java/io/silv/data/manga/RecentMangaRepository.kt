package io.silv.data.manga

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import io.silv.database.entity.manga.SourceMangaResource
import kotlinx.coroutines.flow.Flow

interface RecentMangaRepository {

    fun recentMangaPagingData(config: PagingConfig, ): Flow<PagingData<SourceMangaResource>>
}

