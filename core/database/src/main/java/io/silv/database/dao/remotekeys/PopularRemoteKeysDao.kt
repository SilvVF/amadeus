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
import io.silv.database.entity.manga.remotekeys.PopularRemoteKey

@Dao
interface PopularRemoteKeysDao {

    @Delete
    suspend fun delete(key: PopularRemoteKey)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: PopularRemoteKey)

    @Query("DELETE FROM PopularRemoteKey")
    suspend fun clear()

    @Query("SELECT * FROM PopularRemoteKey WHERE manga_id = :id")
    suspend fun getByMangaId(id: String): PopularRemoteKey

    @Transaction
    @Query("SELECT * FROM PopularRemoteKey")
    fun getPagingSource(): PagingSource<Int, PopularRemoteKeyWithManga>
}

data class PopularRemoteKeyWithManga(

    @Embedded
    override val key: PopularRemoteKey,

    @Relation(
        parentColumn = "manga_id",
        entityColumn = "id"
    )
    override val manga: SourceMangaResource

): RemoteKeyWithManga<PopularRemoteKey>

interface RemoteKeyWithManga<T> {


    val key: T

    val manga: SourceMangaResource
}