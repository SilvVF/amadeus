package io.silv.amadeus.local

import androidx.work.WorkManager
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

    single {
        WorkManager.getInstance(androidContext())
    }

    worker {
        ChapterDownloadWorker(androidContext(), get())
    }
}