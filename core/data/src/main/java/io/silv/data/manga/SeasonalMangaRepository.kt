package io.silv.data.manga

import io.silv.data.util.Syncable
import io.silv.database.entity.manga.resource.SeasonalMangaResource
import io.silv.database.entity.relations.SeasonListWithManga
import kotlinx.coroutines.flow.Flow


interface SeasonalMangaRepository: Syncable {

    fun observeMangaResourceById(id: String): Flow<SeasonalMangaResource?>

    fun observeAllMangaResources(): Flow<List<SeasonalMangaResource>>

    fun getSeasonalLists(): Flow<List<SeasonListWithManga>>
}
