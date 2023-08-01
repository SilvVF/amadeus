package io.silv.manga.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.silv.manga.local.entity.RecentMangaResource
import kotlinx.coroutines.flow.Flow

@Dao
internal interface RecentMangaResourceDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertManga(mangaResource: RecentMangaResource)


    @Query("""
          SELECT * FROM RecentMangaResource ORDER BY savedLocalAtEpochSeconds ASC
    """)
    fun getMangaResources(): Flow<List<RecentMangaResource>>

    @Query("SELECT * FROM RecentMangaResource WHERE id = :mangaId")
    fun getResourceAsFlowById(mangaId: String): Flow<RecentMangaResource?>

    @Query("SELECT * FROM RecentMangaResource")
    suspend fun getAll(): List<RecentMangaResource>

    @Update
    suspend fun update(mangaResource: RecentMangaResource)


    @Query("SELECT * FROM RecentMangaResource WHERE id = :id LIMIT 1")
    suspend fun getMangaById(id: String):  RecentMangaResource?


    @Delete
    suspend fun delete(mangaResource: RecentMangaResource)

    companion object {
        const val id: Int = 6
    }
}

