package io.silv.data.manga

import androidx.paging.Pager
import io.silv.database.dao.remotekeys.PopularRemoteKeyWithManga

interface PopularMangaRepository {

    val pager: Pager<Int, PopularRemoteKeyWithManga>
}

