package io.silv.database

import org.koin.dsl.module

val daosModule = module {

    includes(databaseModule)

    single {
        get<AmadeusDatabase>().chapterDao()
    }

    single {
        get<AmadeusDatabase>().savedMangaDao()
    }

    single {
        get<AmadeusDatabase>().popularRemoteKeysDao()
    }

    single {
        get<AmadeusDatabase>().searchRemoteKeysDao()
    }

    single {
        get<AmadeusDatabase>().recentRemoteKeysDao()
    }

    single {
        get<AmadeusDatabase>().seasonalRemoteKeysDao()
    }

    single {
        get<AmadeusDatabase>().filteredRemoteKeysDao()
    }

    single {
        get<AmadeusDatabase>().filteredYearlyRemoteKeysDao()
    }

    single {
        get<AmadeusDatabase>().seasonalListDao()
    }

    single {
        get<AmadeusDatabase>().tagDao()
    }

    single {
        get<AmadeusDatabase>().quickSearchRemoteKeysDao()
    }

    single {
        get<AmadeusDatabase>().sourceMangaDao()
    }

    single {
        get<AmadeusDatabase>().mangaUpdateDao()
    }
}