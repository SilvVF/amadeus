package io.silv.amadeus

import io.silv.amadeus.ui.screens.screenModule
import io.silv.core.AmadeusDispatchers
import io.silv.manga.di.domainModule
import io.silv.manga.di.mangaModule
import org.koin.dsl.module

val appModule = module {

    includes(
        screenModule,
        domainModule,
        mangaModule
    )

    single<AmadeusDispatchers> {
        AmadeusDispatchers.default
    }
}