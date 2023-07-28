package io.silv.manga.local

import org.koin.dsl.module

val daosModule = module {

    includes(databaseModule)

    single {
        get<AmadeusDatabase>().chapterDao()
    }

    single {
        get<AmadeusDatabase>().mangaDao()
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
}