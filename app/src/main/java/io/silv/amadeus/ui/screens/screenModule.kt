package io.silv.amadeus.ui.screens

import io.silv.amadeus.ui.screens.home.HomeSM
import io.silv.amadeus.ui.screens.library.LibrarySM
import io.silv.amadeus.ui.screens.manga_filter.MangaFilterSM
import io.silv.amadeus.ui.screens.manga_reader.MangaReaderSM
import io.silv.amadeus.ui.screens.manga_view.MangaViewSM
import io.silv.amadeus.ui.screens.search.SearchSM
import io.silv.manga.domain.models.SavableManga
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val screenModule = module {

    factoryOf(::LibrarySM)

    factory {  (mangaId: String, chapterId: String) ->
        MangaReaderSM(
            get(),
            get(),
            get(),
            get(),
            mangaId,
            chapterId,
        )
    }

    factory { (manga: SavableManga) ->
        MangaViewSM(
            get(), get(), get(),
            manga
        )
    }

    factory { (tagId: String) ->
        MangaFilterSM(
            get(),
            get(),
            get(),
            tagId
        )
    }

    factoryOf(::SearchSM)

    factoryOf(::HomeSM)
}