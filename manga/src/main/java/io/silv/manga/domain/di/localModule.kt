package io.silv.manga.domain.di

import androidx.work.WorkManager
import io.silv.manga.local.cache.ChapterImageCache
import io.silv.manga.local.workers.ChapterDownloadWorker
import io.silv.manga.local.workers.CleanupWorker
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module

val localModule = module {

    includes(daosModule)

    single { ChapterImageCache(androidContext(), get()) }

    single {
        WorkManager.getInstance(androidContext())
    }

    worker {
        ChapterDownloadWorker(androidContext(), get())
    }

    worker {
        CleanupWorker(androidContext(), get())
    }
}