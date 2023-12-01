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
import io.silv.database.entity.manga.remotekeys.RemoteKey

@Dao
interface RemoteKeyDao {

    @Delete
    suspend fun delete(key: RemoteKey)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: RemoteKey)

    @Query("DELETE FROM RemoteKey")
    suspend fun clearAllKeys()

    @Query("DELETE FROM RemoteKey WHERE query_id = :query")
    suspend fun clearByQuery(query: String)

    @Query("SELECT * FROM RemoteKey WHERE id = :id")
    suspend fun getById(id: Long): RemoteKey

    @Query("SELECT * FROM RemoteKey WHERE manga_id = :id AND query_id = :query")
    suspend fun getByMangaId(id: String, query: String): RemoteKey

    @Transaction
    @Query("SELECT * FROM RemoteKey WHERE query_id like :query ORDER BY `offset` ASC")
    fun getPagingSourceForQuery(query: String): PagingSource<Int, RemoteKeyWithManga>
}

data class RemoteKeyWithManga(

    @Embedded
    val key: RemoteKey,

    @Relation(
        parentColumn = "manga_id",
        entityColumn = "id"
    )
    val manga: SourceMangaResource
)
