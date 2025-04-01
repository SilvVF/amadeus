package io.silv.domain.manga

import androidx.paging.Pager
import androidx.paging.PagingConfig
import io.silv.common.model.MangaResource
import io.silv.common.model.PagedType

interface MangaPagingSourceFactory {
    fun pager(config: PagingConfig, type: PagedType): Pager<Int, MangaResource>
}