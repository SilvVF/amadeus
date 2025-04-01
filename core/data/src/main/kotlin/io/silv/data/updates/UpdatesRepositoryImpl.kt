package io.silv.data.updates

import io.silv.database.dao.UpdatesDao
import io.silv.domain.update.UpdateWithRelations
import io.silv.domain.update.UpdatesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class UpdatesRepositoryImpl(
    private val updatesDao: UpdatesDao,
): UpdatesRepository {

    override fun observeUpdateCount(): Flow<Int> = updatesDao.mangaWithUpdatesCount()

    override fun observeUpdates(limit: Int): Flow<List<UpdateWithRelations>> =
        updatesDao.updates(limit).map { updates ->
            updates.map { update ->
                UpdateWithRelations(
                    mangaId = update.mangaId,
                    mangaTitle = update.mangaTitle,
                    chapterId = update.chapterId,
                    chapterName = update.chapterName,
                    scanlator = update.scanlator,
                    read = update.read,
                    bookmark = update.bookmarked,
                    lastPageRead = update.lastPageRead,
                    favorite = update.favorite,
                    coverArt = update.coverArt,
                    savedAtLocal = update.savedAtLocal,
                    chapterUpdatedAt = update.chapterUpdatedAt,
                    coverLastModified = update.coverLastModified,
                    chapterNumber = update.chapterNumber
                )
            }
        }

    override fun observeUpdatesByMangaId(id: String): Flow<List<UpdateWithRelations>> =
        updatesDao.updatesByMangaId(id).map { updates ->
            updates.map { update ->
                UpdateWithRelations(
                    mangaId = update.mangaId,
                    mangaTitle = update.mangaTitle,
                    chapterId = update.chapterId,
                    chapterName = update.chapterName,
                    scanlator = update.scanlator,
                    read = update.read,
                    bookmark = update.bookmarked,
                    lastPageRead = update.lastPageRead,
                    favorite = update.favorite,
                    coverArt = update.coverArt,
                    savedAtLocal = update.savedAtLocal,
                    chapterUpdatedAt = update.chapterUpdatedAt,
                    coverLastModified = update.coverLastModified,
                    chapterNumber = update.chapterNumber
                )
            }
        }
}