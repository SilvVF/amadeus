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
        get<AmadeusDatabase>().popularMangaResourceDao()
    }

    single {
        get<AmadeusDatabase>().searchMangaResourceDao()
    }

    single {
        get<AmadeusDatabase>().recentMangaResourceDao()
    }

    single {
        get<AmadeusDatabase>().seasonalMangaResourceDao()
    }

    single {
        get<AmadeusDatabase>().filteredMangaResourceDao()
    }

    single {
        get<AmadeusDatabase>().filteredMangaYearlyResourceDao()
    }

    single {
        get<AmadeusDatabase>().seasonalListDao()
    }

    single {
        get<AmadeusDatabase>().tagDao()
    }

    single {
        get<AmadeusDatabase>().quickSearchResourceDao()
    }

    single {
        get<AmadeusDatabase>().tempMangaResourceDao()
    }

    single {
        get<AmadeusDatabase>().mangaUpdateDao()
    }
}