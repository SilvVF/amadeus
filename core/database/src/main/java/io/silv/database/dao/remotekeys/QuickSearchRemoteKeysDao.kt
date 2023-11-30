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
import io.silv.database.entity.manga.remotekeys.QuickSearchRemoteKey

@Dao
interface QuickSearchRemoteKeysDao {

    @Delete
    suspend fun delete(key: QuickSearchRemoteKey)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: QuickSearchRemoteKey)

    @Query("DELETE FROM QuickSearchRemoteKey")
    suspend fun clear()

    @Query("SELECT * FROM QuickSearchRemoteKey WHERE id = :id")
    suspend fun getByMangaId(id: String): QuickSearchRemoteKey

    @Transaction
    @Query("SELECT * FROM QuickSearchRemoteKey")
    fun getPagingSource(): PagingSource<Int, QuickSearchRemoteKeyWithManga>
}

data class QuickSearchRemoteKeyWithManga(

    @Embedded
    val key: QuickSearchRemoteKey,

    @Relation(
        parentColumn = "manga_id",
        entityColumn = "id"
    )
    val manga: SourceMangaResource
)