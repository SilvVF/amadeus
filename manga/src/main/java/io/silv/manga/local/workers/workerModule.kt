package io.silv.manga.local.workers

import androidx.work.WorkManager
import io.silv.manga.sync.MangaSyncWorkName
import io.silv.manga.sync.SavedMangaSyncManager
import io.silv.manga.sync.SyncManager
import io.silv.manga.sync.TagSyncManager
import io.silv.manga.sync.TagSyncWorkName
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
        named(MangaSyncWorkName)
    }

    single {
        TagSyncManager(androidContext())
    } withOptions {
        bind<SyncManager>()
        named(TagSyncWorkName)
    }

    single {
        WorkManager.getInstance(androidContext())
    }

    single {
        CoverArtDownloadManager(get())
    }

    single {
        TagSyncWorker(androidContext(), get())
    }

    worker {
        CoverArtDeletionWorker(androidContext(), get())
    }

    worker {
        CoverArtDownloadWorker(androidContext(), get())
    }

    worker {
        ChapterDownloadWorker(androidContext(), get())
    }

    worker {
        ChapterDeletionWorker(androidContext(), get())
    }

    worker {
        MangaSyncWorker(androidContext(), get())
    }

    worker {
        ChapterDownloadWorker(androidContext(), get())
    }
}