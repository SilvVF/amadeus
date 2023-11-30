package io.silv.database.dao.remotekeys

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.silv.database.entity.manga.remotekeys.FilteredYearlyRemoteKey

@Dao
interface FilteredYearlyRemoteKeysDao {

    @Delete
    suspend fun delete(key: FilteredYearlyRemoteKey)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: FilteredYearlyRemoteKey)

    @Query("DELETE FROM FilteredYearlyRemoteKey")
    suspend fun clear()

}