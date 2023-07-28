package io.silv.manga.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.silv.manga.local.dao.ChapterDao
import io.silv.manga.local.dao.PopularMangaResourceDao
import io.silv.manga.local.dao.RecentMangaResourceDao
import io.silv.manga.local.dao.SavedMangaDao
import io.silv.manga.local.dao.SearchMangaResourceDao
import io.silv.manga.local.dao.SeasonalMangaResourceDao
import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.local.entity.PopularMangaResource
import io.silv.manga.local.entity.RecentMangaResource
import io.silv.manga.local.entity.SavedMangaEntity
import io.silv.manga.local.entity.SearchMangaResource
import io.silv.manga.local.entity.SeasonalMangaResource
import io.silv.manga.local.entity.converters.Converters

@Database(
    entities = [
        ChapterEntity::class,
        SavedMangaEntity::class,
        RecentMangaResource::class,
        PopularMangaResource::class,
        SearchMangaResource::class,
        SeasonalMangaResource::class
    ],
    version = 1,
)
@TypeConverters(Converters::class)
internal abstract class AmadeusDatabase: RoomDatabase() {

    abstract fun chapterDao(): ChapterDao

    abstract fun mangaDao(): SavedMangaDao

    abstract fun recentMangaResourceDao(): RecentMangaResourceDao

    abstract fun popularMangaResourceDao(): PopularMangaResourceDao

    abstract fun searchMangaResourceDao(): SearchMangaResourceDao

    abstract fun seasonalMangaResourceDao(): SeasonalMangaResourceDao
}