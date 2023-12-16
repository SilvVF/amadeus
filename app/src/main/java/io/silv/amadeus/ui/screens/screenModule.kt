package io.silv.amadeus.ui.screens

import io.silv.explore.ExploreScreenModel
import io.silv.explore.FilterScreenViewModel
import io.silv.library.LibrarySM
import io.silv.manga.manga_filter.MangaFilterScreenModel
import io.silv.manga.manga_view.MangaViewScreenModel
import io.silv.reader.ReaderScreenModel
import io.silv.sync.SeasonalMangaSyncWorkName
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val screenModule = module {

    factoryOf(::LibrarySM)

    factory {(mangaId: String, initialChapterId: String) ->
        ReaderScreenModel(get(),get(), mangaId, initialChapterId, get())
    }

    factoryOf(::MangaViewScreenModel)

    factoryOf(::MangaFilterScreenModel)

    viewModelOf(::FilterScreenViewModel)

    factory {
        ExploreScreenModel(
            subscribeToPagingData = get(),
            savedMangaRepository = get(),
            subscribeToSeasonalLists = get(),
            recentSearchHandler = get(),
            seasonalMangaSyncManager = get(qualifier = named(SeasonalMangaSyncWorkName)),
        )
    }
}