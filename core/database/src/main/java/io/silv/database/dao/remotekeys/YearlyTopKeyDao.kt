package io.silv.database.dao.remotekeys

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import io.silv.database.entity.manga.SourceMangaResource
import io.silv.database.entity.manga.remotekeys.YearlyTopKey
import kotlinx.coroutines.flow.Flow

@Dao
interface YearlyTopKeyDao {

    @Delete
    suspend fun delete(key: YearlyTopKey)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: YearlyTopKey)

    @Query("SELECT * FROM YearlyTopKey WHERE manga_id = :id")
    suspend fun getByMangaId(id: String): YearlyTopKey?

    @Query("DELETE FROM YearlyTopKey")
    suspend fun clear()

    @Query("SELECT * FROM YearlyTopKey WHERE :tagId in (tagIds)")
    fun observeTopYearlyByTagId(tagId: String): Flow<List<YearlyTopKeyWithManga>>
}

data class YearlyTopKeyWithManga(

    @Embedded
    val key: YearlyTopKey,

    @Relation(
        parentColumn = "manga_id",
        entityColumn = "id"
    )
    val manga: SourceMangaResource
)