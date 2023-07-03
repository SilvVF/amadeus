package io.silv.amadeus

import io.silv.amadeus.ui.screens.screenModule
import io.silv.core.AmadeusDispatchers
import io.silv.manga.domain.di.domainModule
import io.silv.manga.domain.di.mangaModule
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