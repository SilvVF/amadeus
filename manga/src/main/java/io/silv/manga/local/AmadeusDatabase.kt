package io.silv.manga.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.silv.manga.local.entity.converters.Converters
import io.silv.manga.local.dao.ChapterDao
import io.silv.manga.local.dao.MangaResourceDao
import io.silv.manga.local.dao.SavedMangaDao
import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.local.entity.MangaResource
import io.silv.manga.local.entity.SavedMangaEntity

@Database(
    entities = [ChapterEntity::class, SavedMangaEntity::class, MangaResource::class],
    version = 1,
)
@TypeConverters(Converters::class)
internal abstract class AmadeusDatabase: RoomDatabase() {

    abstract fun chapterDao(): ChapterDao

    abstract fun mangaDao(): SavedMangaDao

    abstract fun mangaResourceDao(): MangaResourceDao
}