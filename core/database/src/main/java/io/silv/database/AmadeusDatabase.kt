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
import io.silv.database.dao.remotekeys.FilteredYearlyRemoteKeysDao
import io.silv.database.dao.remotekeys.RemoteKeyDao
import io.silv.database.dao.remotekeys.SeasonalRemoteKeysDao
import io.silv.database.entity.chapter.ChapterEntity
import io.silv.database.entity.list.SeasonalListEntity
import io.silv.database.entity.list.TagEntity
import io.silv.database.entity.manga.MangaUpdateEntity
import io.silv.database.entity.manga.SavedMangaEntity
import io.silv.database.entity.manga.SourceMangaResource
import io.silv.database.entity.manga.remotekeys.FilteredYearlyRemoteKey
import io.silv.database.entity.manga.remotekeys.RemoteKey
import io.silv.database.entity.manga.remotekeys.SeasonalRemoteKey

@Database(
    entities = [
        ChapterEntity::class,
        SavedMangaEntity::class,
        SourceMangaResource::class,
        SeasonalRemoteKey::class,
        FilteredYearlyRemoteKey::class,
        SeasonalListEntity::class,
        TagEntity::class,
        MangaUpdateEntity::class,
        RemoteKey::class
    ],
    version = 1,
)
@TypeConverters(Converters::class)
abstract class AmadeusDatabase: RoomDatabase() {

    abstract fun chapterDao(): ChapterDao

    abstract fun savedMangaDao(): SavedMangaDao

    abstract fun sourceMangaDao(): SourceMangaDao

    abstract fun seasonalRemoteKeysDao(): SeasonalRemoteKeysDao


    abstract fun filteredYearlyRemoteKeysDao(): FilteredYearlyRemoteKeysDao

    abstract fun seasonalListDao(): SeasonalListDao

    abstract fun tagDao(): TagDao

    abstract fun mangaUpdateDao(): MangaUpdateDao

    abstract fun remoteKeyDao(): RemoteKeyDao
}