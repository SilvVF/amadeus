package io.silv.amadeus.ui.screens

import io.silv.amadeus.local.localModule
import io.silv.amadeus.ui.screens.manga_view.MangaViewSM
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val screenModule = module {

    includes(localModule)

    factoryOf(::MangaViewSM)

    factoryOf(::HomeSM)
}