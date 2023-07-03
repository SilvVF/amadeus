package io.silv.amadeus.ui.screens

import io.silv.amadeus.ui.screens.home.HomeSM
import io.silv.amadeus.ui.screens.manga_view.MangaViewSM
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val screenModule = module {

    factoryOf(::MangaViewSM)

    factoryOf(::HomeSM)
}