package io.silv.amadeus

import io.silv.data.util.CoverCache
import io.silv.amadeus.ui.screens.screenModule
import io.silv.common.AmadeusDispatchers
import io.silv.common.ApplicationScope
import io.silv.common.model.NetworkConnectivity
import io.silv.data.dataModule
import io.silv.data.util.NetworkConnectivityImpl
import io.silv.datastore.dataStoreModule
import io.silv.domainModule
import io.silv.sync.syncModule
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule =
    module {

        single {
            AmadeusDispatchers.default
        }

        single {
            CoverCache(androidContext())
        }

        single {
            ApplicationScope()
        }

        single<NetworkConnectivity> {
            NetworkConnectivityImpl(androidContext())
        }

        includes(
            listOf(
                dataStoreModule,
                domainModule,
                syncModule,
                dataModule,
                screenModule,
            ),
        )
    }
