package io.silv.database

import android.content.Context
import androidx.room.Room

class DatabaseModule(context: Context) {
    val database: AmadeusDatabase by lazy {
        Room.databaseBuilder(
            context,
            AmadeusDatabase::class.java,
            "amadeus.db",
        )
            .fallbackToDestructiveMigration()
            .build()
    }
}
