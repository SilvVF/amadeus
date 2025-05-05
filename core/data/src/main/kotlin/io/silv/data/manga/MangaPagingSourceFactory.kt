package io.silv.data.manga

import androidx.paging.Pager
import androidx.paging.PagingConfig
import io.silv.common.model.MangaResource
import io.silv.common.model.PagedType
import io.silv.data.manga.model.Manga

interface MangaPagingSourceFactory {
    fun pager(config: PagingConfig, type: PagedType): Pager<Int, Manga>
}