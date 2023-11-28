package io.silv.amadeus.ui.screens

import io.silv.amadeus.ui.screens.home.HomeSM
import io.silv.amadeus.ui.screens.library.LibrarySM
import io.silv.amadeus.ui.screens.manga_filter.MangaFilterSM
import io.silv.amadeus.ui.screens.manga_reader.MangaReaderSM
import io.silv.amadeus.ui.screens.manga_view.MangaViewSM
import io.silv.amadeus.ui.screens.search.SearchSM
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
        HomeSM(
            recentMangaRepository = get(),
            popularMangaRepository = get(),
            seasonalMangaRepository = get(),
            savedMangaRepository = get(),
            searchMangaRepository = get(),
            seasonalMangaSyncManager = get(qualifier = named(SeasonalMangaSyncWorkName))
        )
    }
}