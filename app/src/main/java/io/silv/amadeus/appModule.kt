package io.silv.amadeus

import io.silv.amadeus.network.networkModule
import io.silv.amadeus.ui.screens.screenModule
import org.koin.dsl.module

val appModule = module {

    includes(
        networkModule,
        screenModule,
    )

    single<AmadeusDispatchers> {
        AmadeusDispatchers.default
    }
}