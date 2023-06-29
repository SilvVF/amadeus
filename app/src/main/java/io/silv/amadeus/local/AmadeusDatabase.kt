package io.silv.amadeus.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.silv.amadeus.local.converters.Converters
import io.silv.amadeus.local.dao.ChapterDao
import io.silv.amadeus.local.dao.MangaDao
import io.silv.amadeus.local.dao.VolumeDao
import io.silv.amadeus.local.entity.ChapterEntity
import io.silv.amadeus.local.entity.MangaEntity
import io.silv.amadeus.local.entity.VolumeEntity

@Database(
    entities = [ChapterEntity::class, MangaEntity::class, VolumeEntity::class],
    version = 1,
)
@TypeConverters(Converters::class)
abstract class AmadeusDatabase: RoomDatabase() {

    abstract fun chapterDao(): ChapterDao

    abstract fun mangaDao(): MangaDao

    abstract fun volumeDao(): VolumeDao
}