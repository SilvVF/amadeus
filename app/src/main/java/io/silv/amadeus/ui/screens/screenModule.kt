package io.silv.amadeus.ui.screens

import io.ktor.http.parameters
import io.silv.amadeus.ui.screens.home.HomeSM
import io.silv.amadeus.ui.screens.library.LibrarySM
import io.silv.amadeus.ui.screens.manga_reader.MangaReaderSM
import io.silv.amadeus.ui.screens.manga_view.MangaViewSM
import io.silv.amadeus.ui.screens.saved.SavedMangaSM
import io.silv.manga.domain.models.DomainManga
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.withOptions
import org.koin.core.qualifier.named
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module

val screenModule = module {

    factoryOf(::LibrarySM)

    factory {  (mangaId: String, chapterId: String) ->
        MangaReaderSM(
            get(),
            get(),
            mangaId,
            chapterId
        )
    }

    factory { (manga: DomainManga) ->
        MangaViewSM(
            get(), get(), get(),
            manga
        )
    }

    factoryOf(::HomeSM)

    factoryOf(::SavedMangaSM)
}