package io.silv.manga.domain.di

import androidx.work.WorkManager
import io.silv.manga.local.cache.ChapterImageCache
import io.silv.manga.local.workers.ChapterDownloadWorker
import io.silv.manga.sync.MangaSyncManger
import io.silv.manga.sync.MangaSyncWorker
import io.silv.manga.sync.SyncManager
import io.silv.manga.sync.Synchronizer
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

val localModule = module {

    includes(daosModule)

    single { ChapterImageCache(androidContext(), get()) }

    single {
        MangaSyncManger(androidContext())
    } withOptions {
        bind<SyncManager>()
    }

    single {
        WorkManager.getInstance(androidContext())
    }

    worker {
        MangaSyncWorker(androidContext(), get())
    } withOptions {
        bind<Synchronizer>()
    }

    worker {
        ChapterDownloadWorker(androidContext(), get())
    }
}