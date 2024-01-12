package io.silv.database.dao

import androidx.room.Dao
import androidx.room.Query
import io.silv.database.UpdatesView
import kotlinx.coroutines.flow.Flow

@Dao
interface UpdatesDao {

    @Query("SELECT COUNT(DISTINCT mangaId) FROM updatesview")
    fun mangaWithUpdatesCount(): Flow<Int>

    @Query("SELECT * FROM updatesview LIMIT :limit")
    fun updates(limit: Int = Int.MAX_VALUE): Flow<List<UpdatesView>>

    @Query("SELECT * FROM updatesview WHERE mangaId = :id")
    fun updatesByMangaId(id: String): Flow<List<UpdatesView>>
}