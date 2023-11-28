package io.silv.amadeus

import io.silv.amadeus.data.dataStoreModule
import io.silv.amadeus.manga_usecase.useCaseModule
import io.silv.amadeus.ui.screens.screenModule
import io.silv.common.AmadeusDispatchers
import io.silv.data.dataModule
import io.silv.sync.syncModule
import org.koin.dsl.module

val appModule = module {

    single {
        AmadeusDispatchers.default
    }

    includes(
        listOf(
            dataStoreModule,
            useCaseModule,
            syncModule,
            dataModule,
            screenModule
        )
    )
}