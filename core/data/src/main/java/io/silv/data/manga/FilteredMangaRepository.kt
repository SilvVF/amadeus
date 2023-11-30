package io.silv.data.manga

import androidx.paging.Pager
import io.silv.database.entity.manga.SourceMangaResource

interface FilteredMangaRepository {

    fun pager(query: FilteredResourceQuery): Pager<Int, SourceMangaResource>
}
