package io.silv.manga.sync

import androidx.room.OnConflictStrategy
import androidx.room.Update

internal interface SyncableDao<in E: AmadeusEntity> {

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: E)
}

internal suspend fun <E : AmadeusEntity>  SyncableDao<E>.upsert(vararg entities: E) = entities.forEach { e -> upsert(e) }
