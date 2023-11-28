package io.silv.amadeus

import io.silv.amadeus.ui.screens.screenModule
import io.silv.common.AmadeusDispatchers
import io.silv.data.dataModule
import io.silv.datastore.dataStoreModule
import io.silv.domainModule
import io.silv.sync.syncModule
import org.koin.dsl.module

val appModule = module {

    single {
        AmadeusDispatchers.default
    }

    includes(
        listOf(
            dataStoreModule,
            domainModule,
            syncModule,
            dataModule,
            screenModule
        )
    )
}