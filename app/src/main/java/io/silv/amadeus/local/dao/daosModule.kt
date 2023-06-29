package io.silv.amadeus.local.dao

import io.silv.amadeus.local.AmadeusDatabase
import org.koin.dsl.module

val daosModule = module {

    single {
        get<AmadeusDatabase>().chapterDao()
    }

    single {
        get<AmadeusDatabase>().mangaDao()
    }

    single {
        get<AmadeusDatabase>().volumeDao()
    }
}