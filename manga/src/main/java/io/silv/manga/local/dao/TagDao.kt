package io.silv.manga.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.silv.manga.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface TagDao {

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun upsert(tag: TagEntity)

    @Update
    suspend fun update(tag: TagEntity)

    @Delete
    suspend fun delete(tag: TagEntity)

    @Query("SELECT * FROM tagentity")
    fun getAllAsFlow(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tagentity")
    suspend fun getAll(): List<TagEntity>
}