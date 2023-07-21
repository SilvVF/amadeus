package io.silv.manga.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.silv.manga.local.entity.SearchMangaResource
import kotlinx.coroutines.flow.Flow


@Dao
internal interface SearchMangaResourceDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertManga(mangaResource: SearchMangaResource)


    @Query("""
          SELECT * FROM SearchMangaResource ORDER BY savedLocalAtEpochSeconds ASC
    """)
    fun getMangaResources(): Flow<List<SearchMangaResource>>

    @Query("SELECT * FROM SearchMangaResource WHERE id = :mangaId")
    fun getResourceAsFlowById(mangaId: String): Flow<SearchMangaResource?>

    @Query("SELECT * FROM SearchMangaResource")
    suspend fun getAll(): List<SearchMangaResource>

    @Update
    suspend fun update(mangaResource: SearchMangaResource)


    @Query("SELECT * FROM SearchMangaResource WHERE id = :id LIMIT 1")
    suspend fun getMangaById(id: String):  SearchMangaResource?


    @Delete
    suspend fun delete(mangaResource: SearchMangaResource)

    companion object {
        const val id = 2
    }
}
