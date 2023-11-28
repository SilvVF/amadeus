package io.silv.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.silv.database.dao.ChapterDao
import io.silv.database.dao.FilteredMangaResourceDao
import io.silv.database.dao.FilteredMangaYearlyResourceDao
import io.silv.database.dao.MangaUpdateDao
import io.silv.database.dao.PopularMangaResourceDao
import io.silv.database.dao.QuickSearchMangaResourceDao
import io.silv.database.dao.RecentMangaResourceDao
import io.silv.database.dao.SavedMangaDao
import io.silv.database.dao.SearchMangaResourceDao
import io.silv.database.dao.SeasonalListDao
import io.silv.database.dao.SeasonalMangaResourceDao
import io.silv.database.dao.TagDao
import io.silv.database.dao.TempMangaResourceDao
import io.silv.database.entity.chapter.ChapterEntity
import io.silv.database.entity.list.SeasonalListEntity
import io.silv.database.entity.list.TagEntity
import io.silv.database.entity.manga.MangaUpdateEntity
import io.silv.database.entity.manga.SavedMangaEntity
import io.silv.database.entity.manga.resource.FilteredMangaResource
import io.silv.database.entity.manga.resource.FilteredMangaYearlyResource
import io.silv.database.entity.manga.resource.PopularMangaResource
import io.silv.database.entity.manga.resource.QuickSearchMangaResource
import io.silv.database.entity.manga.resource.RecentMangaResource
import io.silv.database.entity.manga.resource.SearchMangaResource
import io.silv.database.entity.manga.resource.SeasonalMangaResource
import io.silv.database.entity.manga.resource.TempMangaResource

@Database(
    entities = [
        ChapterEntity::class,
        SavedMangaEntity::class,
        RecentMangaResource::class,
        PopularMangaResource::class,
        SearchMangaResource::class,
        SeasonalMangaResource::class,
        FilteredMangaResource::class,
        FilteredMangaYearlyResource::class,
        SeasonalListEntity::class,
        TagEntity::class,
        QuickSearchMangaResource::class,
        TempMangaResource::class,
        MangaUpdateEntity::class
    ],
    version = 1,
)
@TypeConverters(Converters::class)
abstract class AmadeusDatabase: RoomDatabase() {

    abstract fun chapterDao(): ChapterDao

    abstract fun savedMangaDao(): SavedMangaDao

    abstract fun recentMangaResourceDao(): RecentMangaResourceDao

    abstract fun popularMangaResourceDao(): PopularMangaResourceDao

    abstract fun searchMangaResourceDao(): SearchMangaResourceDao

    abstract fun seasonalMangaResourceDao(): SeasonalMangaResourceDao

    abstract fun filteredMangaResourceDao(): FilteredMangaResourceDao

    abstract fun filteredMangaYearlyResourceDao(): FilteredMangaYearlyResourceDao

    abstract fun seasonalListDao(): SeasonalListDao

    abstract fun tagDao(): TagDao

    abstract fun quickSearchResourceDao(): QuickSearchMangaResourceDao

    abstract fun tempMangaResourceDao(): TempMangaResourceDao

    abstract fun mangaUpdateDao(): MangaUpdateDao
}