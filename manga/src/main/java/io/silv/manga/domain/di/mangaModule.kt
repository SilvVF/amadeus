package io.silv.manga.domain.di

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