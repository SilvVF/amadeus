package io.silv.data.update

import kotlinx.coroutines.flow.Flow

interface UpdatesRepository {

    fun observeUpdateCount(): Flow<Int>

    fun observeUpdates(limit: Int = Int.MAX_VALUE): Flow<List<UpdateWithRelations>>

    fun observeUpdatesByMangaId(id: String): Flow<List<UpdateWithRelations>>
}