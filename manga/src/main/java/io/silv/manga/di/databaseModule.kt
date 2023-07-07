package io.silv.manga.di

import androidx.room.Room
import io.silv.manga.local.AmadeusDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {

    single {
        Room.databaseBuilder(
            androidContext(),
            AmadeusDatabase::class.java,
            "amadeus.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
}