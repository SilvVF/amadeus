package io.silv.database

import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule =
    module {

        single<AmadeusDatabase> {
            Room.databaseBuilder(
                androidContext(),
                AmadeusDatabase::class.java,
                "amadeus.db",
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
