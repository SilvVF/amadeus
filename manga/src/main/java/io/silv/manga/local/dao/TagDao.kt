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
    suspend fun upsertTag(tag: TagEntity)

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Query("SELECT * FROM tagentity")
    fun getAllTags(): Flow<List<TagEntity>>
}