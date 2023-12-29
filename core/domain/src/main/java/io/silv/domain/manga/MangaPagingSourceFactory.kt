package io.silv.domain.manga

import androidx.paging.Pager
import androidx.paging.PagingConfig
import io.silv.common.model.PagedType
import io.silv.domain.manga.model.Manga

interface MangaPagingSourceFactory {
    fun pager(config: PagingConfig, type: PagedType): Pager<Int, Manga>
}