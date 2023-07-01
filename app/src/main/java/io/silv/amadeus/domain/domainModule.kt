package io.silv.amadeus.domain

import io.silv.amadeus.domain.repos.MangaRepo
import io.silv.amadeus.network.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val domainModule = module {

    includes(networkModule)

    single {
        MangaRepo(get())
    }
}