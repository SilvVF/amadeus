package io.silv.manga.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.silv.manga.local.entity.MangaResource
import io.silv.manga.local.entity.SavedMangaEntity
import io.silv.manga.sync.SyncableDao
import kotlinx.coroutines.flow.Flow

@Dao
internal interface MangaResourceDao: SyncableDao<MangaResource> {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertManga(mangaResource: MangaResource)


    @Query("""
          SELECT * FROM mangaresource ORDER BY savedLocalAtEpochSeconds ASC
    """)
    fun getMangaResources(): Flow<List<MangaResource>>

    @Query("SELECT * FROM mangaresource WHERE id = :mangaId")
    fun getResourceAsFlowById(mangaId: String): Flow<MangaResource>

    @Query("SELECT * FROM mangaresource")
    suspend fun getAll(): List<MangaResource>

    @Update
    suspend fun update(mangaResource: MangaResource)


    @Query("SELECT * FROM mangaresource WHERE id = :id LIMIT 1")
    suspend fun getMangaById(id: String):  MangaResource?


    @Delete
    suspend fun delete(mangaResource: MangaResource)
}