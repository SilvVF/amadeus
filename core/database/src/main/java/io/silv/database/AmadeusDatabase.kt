package io.silv.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.silv.database.dao.ChapterDao
import io.silv.database.dao.HistoryDao
import io.silv.database.dao.MangaDao
import io.silv.database.dao.MangaUpdateDao
import io.silv.database.dao.RecentSearchDao
import io.silv.database.dao.SeasonalKeysDao
import io.silv.database.dao.SeasonalListDao
import io.silv.database.dao.TagDao
import io.silv.database.dao.UserListDao
import io.silv.database.entity.RecentSearchEntity
import io.silv.database.entity.chapter.ChapterEntity
import io.silv.database.entity.history.HistoryEntity
import io.silv.database.entity.history.HistoryView
import io.silv.database.entity.list.SeasonalListEntity
import io.silv.database.entity.list.TagEntity
import io.silv.database.entity.list.UserListEntity
import io.silv.database.entity.manga.MangaEntity
import io.silv.database.entity.manga.MangaToListRelation
import io.silv.database.entity.manga.MangaUpdateEntity

@Database(
    entities = [
        ChapterEntity::class,
        MangaEntity::class,
        MangaToListRelation::class,
        SeasonalListEntity::class,
        TagEntity::class,
        MangaUpdateEntity::class,
        RecentSearchEntity::class,
        UserListEntity::class,
        HistoryEntity::class
    ],
    views = [HistoryView::class],
    version = 9,
)
@TypeConverters(Converters::class)
abstract class AmadeusDatabase : RoomDatabase() {
    abstract fun chapterDao(): ChapterDao

    abstract fun sourceMangaDao(): MangaDao

    abstract fun seasonalRemoteKeysDao(): SeasonalKeysDao

    abstract fun seasonalListDao(): SeasonalListDao

    abstract fun tagDao(): TagDao

    abstract fun mangaUpdateDao(): MangaUpdateDao

    abstract fun recentSearchDao(): RecentSearchDao

    abstract fun userListDao(): UserListDao

    abstract fun historyDao(): HistoryDao
}
