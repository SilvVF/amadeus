package io.silv.domain.update

import kotlinx.coroutines.flow.Flow

interface UpdatesRepository {

    fun observeUpdates(limit: Int = Int.MAX_VALUE): Flow<List<UpdateWithRelations>>

    fun observeUpdatesByMangaId(id: String): Flow<List<UpdateWithRelations>>
}