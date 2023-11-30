package io.silv.data.manga

import androidx.paging.Pager
import io.silv.database.dao.remotekeys.QuickSearchRemoteKeyWithManga

interface QuickSearchMangaRepository {

    fun pager(query: String): Pager<Int, QuickSearchRemoteKeyWithManga>
}


