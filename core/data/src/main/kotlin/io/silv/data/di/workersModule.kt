package io.silv.data.di

import androidx.work.WorkManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val workersModule = module {

    single {
        WorkManager.getInstance(androidContext())
    }
}