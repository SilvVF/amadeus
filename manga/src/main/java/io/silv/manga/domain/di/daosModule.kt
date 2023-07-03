package io.silv.manga.domain.di

import io.silv.manga.local.AmadeusDatabase
import org.koin.dsl.module

val daosModule = module {

    includes(databaseModule)

    single {
        get<AmadeusDatabase>().chapterDao()
    }

    single {
        get<AmadeusDatabase>().mangaDao()
    }

}