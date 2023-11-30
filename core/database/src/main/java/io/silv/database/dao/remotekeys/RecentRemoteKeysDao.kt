package io.silv.database.dao.remotekeys

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import io.silv.database.entity.manga.SourceMangaResource
import io.silv.database.entity.manga.remotekeys.RecentMangaRemoteKey


@Dao
interface RecentRemoteKeysDao {

    @Delete
    suspend fun delete(key: RecentMangaRemoteKey)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: RecentMangaRemoteKey)

    @Query("DELETE FROM RecentMangaRemoteKey")
    suspend fun clear()

    @Query("SELECT * FROM RecentMangaRemoteKey WHERE manga_id = :id")
    suspend fun getByMangaId(id: String): RecentMangaRemoteKey

    @Transaction
    @Query("SELECT * FROM RecentMangaRemoteKey")
    fun getPagingSource(): PagingSource<Int, RecentRemoteKeyWithSourceManga>
}

data class RecentRemoteKeyWithSourceManga(

    @Embedded
    val key: RecentMangaRemoteKey,

    @Relation(
        parentColumn = "manga_id",
        entityColumn = "id",
    )
    val manga: SourceMangaResource
)