package io.silv.datastore

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

val dataStoreModule = module {

    single { androidContext().dataStore }

    singleOf(::UserSettingsStoreImpl) withOptions {
        bind<UserSettingsStore>()
    }
}