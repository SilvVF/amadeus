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
    suspend fun upsertRecentMangaResource(mangaResource: RecentMangaResource)


    @Query("SELECT * FROM RecentMangaResource")
    fun getRecentMangaResources(): Flow<List<RecentMangaResource>>

    @Query("SELECT * FROM RecentMangaResource WHERE id = :mangaId")
    fun observeRecentMangaResourceById(mangaId: String): Flow<RecentMangaResource?>

    @Update
    suspend fun updateRecentMangaResource(mangaResource: RecentMangaResource)

    @Delete
    suspend fun deleteRecentMangaResource(mangaResource: RecentMangaResource)

    companion object {
        const val id: Int = 6
    }
}

