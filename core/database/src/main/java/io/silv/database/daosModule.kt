package io.silv.database

class DaosModule(
    private val databaseModule: DatabaseModule
) {
    val db get() = databaseModule.database

    val chapterDao by lazy { db.chapterDao() }
    val seasonRemoteKeys by lazy { db.seasonalRemoteKeysDao() }
    val seasonalListDao by lazy { db.seasonalListDao() }
    val tagDao by lazy { db.tagDao() }
    val sourceMangaDao by lazy { db.sourceMangaDao() }
    val updatesDao by lazy { db.updatesDao() }
    val recentSearchDao by lazy { db.recentSearchDao() }
    val historyDao by lazy { db.historyDao() }
}

