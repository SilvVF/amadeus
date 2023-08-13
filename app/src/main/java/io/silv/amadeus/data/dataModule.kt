package io.silv.amadeus.data

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

val dataModule = module {

    singleOf(::UserSettingsStoreImpl) withOptions {
        bind<UserSettingsStore>()
    }

    single {
        androidContext().dataStore
    }
}