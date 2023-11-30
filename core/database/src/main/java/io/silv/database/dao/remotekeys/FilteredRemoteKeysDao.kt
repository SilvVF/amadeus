package io.silv.database.dao.remotekeys

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.silv.database.entity.manga.SourceMangaResource
import io.silv.database.entity.manga.remotekeys.FilteredRemoteKey

@Dao
interface FilteredRemoteKeysDao {

    @Delete
    suspend fun delete(key: FilteredRemoteKey)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: FilteredRemoteKey)

    @Query("DELETE FROM FilteredRemoteKey")
    suspend fun clear()

    @Query("SELECT * FROM FilteredRemoteKey WHERE manga_id = :id")
    suspend fun getByMangaId(id: String): FilteredRemoteKey

    @Query("""
        select *
        from SourceMangaResource
        where id in (
            select manga_id from FilteredRemoteKey
        )
    """)
    fun getPagingSource(): PagingSource<Int, SourceMangaResource>
}