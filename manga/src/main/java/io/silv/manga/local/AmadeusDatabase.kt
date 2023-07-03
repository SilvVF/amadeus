package io.silv.manga.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.silv.manga.local.converters.Converters
import io.silv.manga.local.dao.ChapterDao
import io.silv.manga.local.dao.MangaDao
import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.local.entity.MangaEntity

@Database(
    entities = [ChapterEntity::class, MangaEntity::class],
    version = 1,
)
@TypeConverters(Converters::class)
abstract class AmadeusDatabase: RoomDatabase() {

    abstract fun chapterDao(): ChapterDao

    abstract fun mangaDao(): MangaDao
}