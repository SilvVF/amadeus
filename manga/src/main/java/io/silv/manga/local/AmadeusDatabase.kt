package io.silv.manga.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.silv.manga.local.dao.ChapterDao
import io.silv.manga.local.dao.FilteredMangaResourceDao
import io.silv.manga.local.dao.FilteredMangaYearlyResourceDao
import io.silv.manga.local.dao.PopularMangaResourceDao
import io.silv.manga.local.dao.QuickSearchMangaResourceDao
import io.silv.manga.local.dao.RecentMangaResourceDao
import io.silv.manga.local.dao.SavedMangaDao
import io.silv.manga.local.dao.SearchMangaResourceDao
import io.silv.manga.local.dao.SeasonalListDao
import io.silv.manga.local.dao.SeasonalMangaResourceDao
import io.silv.manga.local.dao.TagDao
import io.silv.manga.local.dao.TempMangaResourceDao
import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.local.entity.SavedMangaEntity
import io.silv.manga.local.entity.SeasonalListEntity
import io.silv.manga.local.entity.TagEntity
import io.silv.manga.local.entity.converters.Converters
import io.silv.manga.local.entity.manga_resource.FilteredMangaResource
import io.silv.manga.local.entity.manga_resource.FilteredMangaYearlyResource
import io.silv.manga.local.entity.manga_resource.PopularMangaResource
import io.silv.manga.local.entity.manga_resource.QuickSearchMangaResource
import io.silv.manga.local.entity.manga_resource.RecentMangaResource
import io.silv.manga.local.entity.manga_resource.SearchMangaResource
import io.silv.manga.local.entity.manga_resource.SeasonalMangaResource
import io.silv.manga.local.entity.manga_resource.TempMangaResource

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
        TempMangaResource::class
    ],
    version = 1,
)
@TypeConverters(Converters::class)
internal abstract class AmadeusDatabase: RoomDatabase() {

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
}