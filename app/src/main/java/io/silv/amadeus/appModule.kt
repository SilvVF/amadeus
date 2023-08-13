package io.silv.amadeus

import io.silv.amadeus.data.dataModule
import io.silv.amadeus.ui.screens.screenModule
import io.silv.core.AmadeusDispatchers
import io.silv.manga.mangaModule
import org.koin.dsl.module

val appModule = module {

    includes(
        screenModule,
        mangaModule,
        dataModule
    )

    single<AmadeusDispatchers> {
        AmadeusDispatchers.default
    }
}