package io.silv.data.di

import androidx.work.WorkManager
import io.silv.data.workers.chapters.ChapterDeletionWorker
import io.silv.data.workers.chapters.ChapterDownloadWorker
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module

val workersModule = module {

    single {
        WorkManager.getInstance(androidContext())
    }

    worker {
        ChapterDownloadWorker(androidContext(), get())
    }

    worker {
        ChapterDeletionWorker(androidContext(), get())
    }

}