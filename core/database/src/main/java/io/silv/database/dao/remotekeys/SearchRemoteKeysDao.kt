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
import io.silv.database.entity.manga.remotekeys.SearchRemoteKey

@Dao
interface SearchRemoteKeysDao {

    @Delete
    suspend fun delete(key: SearchRemoteKey)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: SearchRemoteKey)

    @Query("DELETE FROM SearchRemoteKey")
    suspend fun clear()

    @Query("SELECT * FROM SearchRemoteKey WHERE manga_id = :id")
    suspend fun getByMangaId(id: String): SearchRemoteKey

    @Transaction
    @Query("SELECT * FROM SearchRemoteKey")
    fun getPagingSource(): PagingSource<Int, SearchRemoteKeyWithManga>
}

data class SearchRemoteKeyWithManga(
    @Embedded
    override val key: SearchRemoteKey,

    @Relation(
        parentColumn = "manga_id",
        entityColumn = "id"
    )
    override val manga: SourceMangaResource

): RemoteKeyWithManga<SearchRemoteKey>