package io.silv.amadeus.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.silv.amadeus.local.entity.VolumeEntity
import io.silv.amadeus.local.relations.VolumeWithChapters

@Dao
interface VolumeDao {

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun upsertVolume(volumeEntity: VolumeEntity)


    @Query("""
        SELECT * FROM volumeentity
        WHERE vid = :id 
        LIMIT 1
    """)
    suspend fun getVolumeById(id: String): VolumeEntity?

    @Transaction
    @Query("""
        SELECT * FROM volumeentity
        WHERE vid = :id
        LIMIT 1
    """)
    fun getVolumeWithChapters(id: String): VolumeWithChapters?
}