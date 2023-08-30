package io.silv.manga.repositorys.manga

import io.silv.manga.local.entity.manga_resource.SeasonalMangaResource
import io.silv.manga.local.entity.relations.SeasonListWithManga
import io.silv.manga.sync.Syncable
import kotlinx.coroutines.flow.Flow


interface SeasonalMangaRepository: Syncable {

    fun observeMangaResourceById(id: String): Flow<SeasonalMangaResource?>

    fun observeAllMangaResources(): Flow<List<SeasonalMangaResource>>

    fun getSeasonalLists(): Flow<List<SeasonListWithManga>>
}
