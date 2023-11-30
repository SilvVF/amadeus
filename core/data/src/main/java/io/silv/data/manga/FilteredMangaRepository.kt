package io.silv.data.manga

import androidx.paging.Pager
import io.silv.database.dao.remotekeys.FilteredRemoteKeyWithManga

interface FilteredMangaRepository {

    fun pager(query: FilteredResourceQuery): Pager<Int, FilteredRemoteKeyWithManga>
}
