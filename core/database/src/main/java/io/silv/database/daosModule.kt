package io.silv.database

import org.koin.dsl.module

val daosModule =
    module {

        includes(databaseModule)

        single {
            get<AmadeusDatabase>().chapterDao()
        }

        single {
            get<AmadeusDatabase>().seasonalRemoteKeysDao()
        }

        single {
            get<AmadeusDatabase>().seasonalListDao()
        }

        single {
            get<AmadeusDatabase>().tagDao()
        }

        single {
            get<AmadeusDatabase>().sourceMangaDao()
        }

        single {
            get<AmadeusDatabase>().updatesDao()
        }

        single {
            get<AmadeusDatabase>().recentSearchDao()
        }

        single {
            get<AmadeusDatabase>().historyDao()
        }

        single {
            get<AmadeusDatabase>().userListDao()
        }
    }
