package io.silv.data.manga

import io.silv.data.util.Syncable
import io.silv.database.entity.list.SeasonalListEntity
import io.silv.database.entity.manga.MangaEntity
import kotlinx.coroutines.flow.Flow


interface SeasonalMangaRepository: Syncable {

    fun subscribe(): Flow<List<Pair<SeasonalListEntity, List<MangaEntity>>>>
}
