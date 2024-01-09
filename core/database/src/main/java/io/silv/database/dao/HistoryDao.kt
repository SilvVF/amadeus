package io.silv.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.silv.database.entity.history.HistoryEntity
import io.silv.database.entity.history.HistoryView
import kotlinx.datetime.LocalDateTime

@Dao
abstract class HistoryDao {

    /**
     * Should call [upsert] instead which will insert or update the entity
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insert(historyEntity: HistoryEntity): Long


    @Query(
        """
            UPDATE history SET 
                last_read = :readAt,
                time_read = time_read + :timeRead
            WHERE chapter_id = :chapterId
        """
    )
    protected abstract suspend fun update(chapterId: String, readAt: LocalDateTime, timeRead: Long)

    /**
     * inserts a new HistoryEntity if it does not exist other wise updates [HistoryEntity.lastRead]
     * and [HistoryEntity.timeRead].
     */
    @Transaction
    open suspend fun upsert(chapterId: String, readAt: LocalDateTime, timeRead: Long) {
        val res = insert(HistoryEntity(chapterId = chapterId, lastRead = readAt, timeRead = timeRead))
        if (res == -1L) {
            update(chapterId, readAt, timeRead)
        }
    }

    @Delete
    abstract suspend fun delete(historyEntity: HistoryEntity)

    @Transaction
    @Query("""
        SELECT *
        FROM history H
        JOIN chapters C
        ON H.chapter_id = C.id
        WHERE C.manga_id = :mangaId AND C.id = H.chapter_id;
    """)
    abstract suspend fun getHistoryByMangaId(mangaId: String): List<HistoryEntity>

    @Query("SELECT * FROM historyview ORDER BY lastRead DESC")
    abstract suspend fun history(): List<HistoryView>
}