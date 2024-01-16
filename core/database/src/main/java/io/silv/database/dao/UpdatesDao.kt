package io.silv.database.dao

import androidx.room.Dao
import androidx.room.Query
import io.silv.common.time.epochMillis
import io.silv.common.time.localDateTimeNow
import io.silv.database.UpdatesView
import kotlinx.coroutines.flow.Flow

@Dao
interface UpdatesDao {

    @Query("""
        SELECT COUNT(DISTINCT mangaId)
        FROM updatesview AS U 
        JOIN chapters AS C ON C.id = U.chapterId
        WHERE (:dateMillis - C.updated_at) < 86400000
    """)
    fun mangaWithUpdatesCount(dateMillis: Long = localDateTimeNow().epochMillis()): Flow<Int>

    @Query("SELECT * FROM updatesview LIMIT :limit")
    fun updates(limit: Int = Int.MAX_VALUE): Flow<List<UpdatesView>>

    @Query("SELECT * FROM updatesview WHERE mangaId = :id")
    fun updatesByMangaId(id: String): Flow<List<UpdatesView>>
}