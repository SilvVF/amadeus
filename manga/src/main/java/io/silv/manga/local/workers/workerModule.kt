package io.silv.manga.local.workers

import androidx.work.WorkManager
import io.silv.manga.sync.SavedMangaSyncManager
import io.silv.manga.sync.SyncManager
import io.silv.manga.sync.Synchronizer
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.named
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

val workerModule = module {
    single {
        SavedMangaSyncManager(androidContext())
    } withOptions {
        bind<SyncManager>()
        named("Manga")
    }

    single {
        WorkManager.getInstance(androidContext())
    }

    worker {
        ChapterDownloadWorker(androidContext(), get())
    }

    worker {
        ChapterDeletionWorker(androidContext(), get())
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