package io.silv.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.silv.database.dao.ChapterDao
import io.silv.database.dao.MangaUpdateDao
import io.silv.database.dao.SavedMangaDao
import io.silv.database.dao.SeasonalListDao
import io.silv.database.dao.SourceMangaDao
import io.silv.database.dao.TagDao
import io.silv.database.dao.remotekeys.FilteredRemoteKeysDao
import io.silv.database.dao.remotekeys.FilteredYearlyRemoteKeysDao
import io.silv.database.dao.remotekeys.PopularRemoteKeysDao
import io.silv.database.dao.remotekeys.QuickSearchRemoteKeysDao
import io.silv.database.dao.remotekeys.RecentRemoteKeysDao
import io.silv.database.dao.remotekeys.SearchRemoteKeysDao
import io.silv.database.dao.remotekeys.SeasonalRemoteKeysDao
import io.silv.database.entity.chapter.ChapterEntity
import io.silv.database.entity.list.SeasonalListEntity
import io.silv.database.entity.list.TagEntity
import io.silv.database.entity.manga.MangaUpdateEntity
import io.silv.database.entity.manga.SavedMangaEntity
import io.silv.database.entity.manga.SourceMangaResource
import io.silv.database.entity.manga.remotekeys.FilteredRemoteKey
import io.silv.database.entity.manga.remotekeys.FilteredYearlyRemoteKey
import io.silv.database.entity.manga.remotekeys.PopularRemoteKey
import io.silv.database.entity.manga.remotekeys.QuickSearchRemoteKey
import io.silv.database.entity.manga.remotekeys.RecentMangaRemoteKey
import io.silv.database.entity.manga.remotekeys.SearchRemoteKey
import io.silv.database.entity.manga.remotekeys.SeasonalRemoteKey

@Database(
    entities = [
        ChapterEntity::class,
        SavedMangaEntity::class,
        SourceMangaResource::class,
        RecentMangaRemoteKey::class,
        PopularRemoteKey::class,
        SearchRemoteKey::class,
        SeasonalRemoteKey::class,
        FilteredRemoteKey::class,
        FilteredYearlyRemoteKey::class,
        SeasonalListEntity::class,
        TagEntity::class,
        QuickSearchRemoteKey::class,
        MangaUpdateEntity::class
    ],
    version = 1,
)
@TypeConverters(Converters::class)
abstract class AmadeusDatabase: RoomDatabase() {

    abstract fun chapterDao(): ChapterDao

    abstract fun savedMangaDao(): SavedMangaDao

    abstract fun sourceMangaDao(): SourceMangaDao

    abstract fun recentRemoteKeysDao(): RecentRemoteKeysDao

    abstract fun popularRemoteKeysDao(): PopularRemoteKeysDao

    abstract fun searchRemoteKeysDao(): SearchRemoteKeysDao

    abstract fun seasonalRemoteKeysDao(): SeasonalRemoteKeysDao

    abstract fun filteredRemoteKeysDao(): FilteredRemoteKeysDao

    abstract fun filteredYearlyRemoteKeysDao(): FilteredYearlyRemoteKeysDao

    abstract fun quickSearchRemoteKeysDao(): QuickSearchRemoteKeysDao

    abstract fun seasonalListDao(): SeasonalListDao

    abstract fun tagDao(): TagDao

    abstract fun mangaUpdateDao(): MangaUpdateDao
}