package io.silv.amadeus.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.silv.amadeus.local.entity.MangaEntity
import io.silv.amadeus.local.relations.MangaWithVolumes

@Dao
interface MangaDao {

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun upsertManga(mangaEntity: MangaEntity)


    @Query("""
       SELECT * FROM mangaentity
       WHERE mid = :id
       LIMIT 1
    """)
    suspend fun getMangaById(id: String): MangaEntity?

    @Transaction
    @Query("""
      SELECT * FROM mangaentity
      WHERE mid = :id
      LIMIT 1
    """)
    suspend fun getMangaWithVolumes(id: String): MangaWithVolumes?
//
//    @Transaction
//    @Query("""
//      SELECT * FROM mangaentity
//      WHERE mid = :id
//      LIMIT 1
//    """)
//    suspend fun getMangaWithVolumesAndChapters(id: String): MangaWithVolumesAndChapters?
}