package io.silv.data.manga

import androidx.paging.Pager
import io.silv.database.dao.remotekeys.RecentRemoteKeyWithSourceManga

interface RecentMangaRepository {

    val pager: Pager<Int, RecentRemoteKeyWithSourceManga>
}

