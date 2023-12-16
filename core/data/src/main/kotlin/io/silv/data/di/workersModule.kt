package io.silv.data.di

import androidx.work.WorkManager
import io.silv.data.download.DownloadWorker
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module

val workersModule = module {

    single {
        WorkManager.getInstance(androidContext())
    }

   worker {
       DownloadWorker(androidContext(), get())
   }
}