package io.silv.amadeus.local

import io.silv.amadeus.local.cache.ChapterImageCache
import io.silv.amadeus.local.dao.daosModule
import io.silv.amadeus.local.workers.ChapterDownloadWorker
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module

val localModule = module {

    includes(daosModule)
    includes(databaseModule)

    single { ChapterImageCache(androidContext(), get()) }

    worker {
        ChapterDownloadWorker(androidContext(), get())
    }
}