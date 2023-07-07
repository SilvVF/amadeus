package io.silv.manga.di

import org.koin.dsl.module

val mangaModule = module {
    includes(
        daosModule,
        databaseModule,
        domainModule,
        localModule,
        networkModule,
    )
}