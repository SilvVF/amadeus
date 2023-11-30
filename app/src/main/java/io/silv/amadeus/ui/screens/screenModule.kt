package io.silv.amadeus.ui.screens

import io.silv.explore.ExploreScreenModel
import io.silv.library.LibrarySM
import io.silv.manga.manga_filter.MangaFilterSM
import io.silv.manga.manga_view.MangaViewSM
import io.silv.manga.search.SearchSM
import io.silv.reader.MangaReaderSM
import io.silv.sync.SeasonalMangaSyncWorkName
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val screenModule = module {

    factoryOf(::LibrarySM)

    factory {(mangaId: String, initialChapterId: String) ->
        MangaReaderSM(get(), get(), get(), get(), mangaId, initialChapterId)
    }

    factoryOf(::MangaViewSM)

    factoryOf(::MangaFilterSM)

    factoryOf(::SearchSM)

    factory {
        ExploreScreenModel(
            recentMangaRepository = get(),
            popularMangaRepository = get(),
            seasonalMangaRepository = get(),
            savedMangaRepository = get(),
            searchMangaRepository = get(),
            seasonalMangaSyncManager = get(qualifier = named(SeasonalMangaSyncWorkName)),
            combineSourceMangaWithSaved = get(),
            getQueryPagingData = get()
        )
    }
}