package io.silv.amadeus.ui.screens

import io.silv.amadeus.ui.screens.home.HomeSM
import io.silv.amadeus.ui.screens.manga_view.MangaViewSM
import io.silv.manga.domain.models.DomainManga
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.withOptions
import org.koin.core.qualifier.named
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module

val screenModule = module {

    factory { (manga: DomainManga) ->
        MangaViewSM(
            get(qualifier("ChapterInfo")),
            get(),
            manga
        )
    }

    factory {
        HomeSM(
            get(),
            get(),
            get(qualifier("Manga"))
        )
    }
}