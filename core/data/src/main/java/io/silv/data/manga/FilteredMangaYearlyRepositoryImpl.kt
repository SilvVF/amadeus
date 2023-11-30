package io.silv.data.manga

import io.silv.common.model.LoadState
import io.silv.data.mappers.toSourceManga
import io.silv.data.util.createSyncer
import io.silv.database.dao.SourceMangaDao
import io.silv.database.dao.remotekeys.FilteredYearlyRemoteKeysDao
import io.silv.database.entity.manga.SourceMangaResource
import io.silv.network.MangaDexApi
import io.silv.network.model.manga.Manga
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FilteredYearlyMangaRepositoryImpl(
    private val remoteKeysDao: FilteredYearlyRemoteKeysDao,
    private val sourceMangaDao: SourceMangaDao,
    private val mangaDexApi: MangaDexApi,
    dispatchers: io.silv.common.AmadeusDispatchers,
): FilteredYearlyMangaRepository {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    override val loadState = MutableStateFlow<LoadState>(LoadState.None)

    private fun syncerForYearly(tag: String) = createSyncer<SourceMangaResource, Pair<Manga, Int>, String>(
            networkToKey = { (manga, _) -> manga.id },
            mapper = { (manga, _), prev ->
                manga.toSourceManga()
            },
            upsert = { manga ->
                sourceMangaDao.insert(manga)
            }
        )

    override fun getYearlyTopResources(tag: String): Flow<List<SourceMangaResource>> {
        return sourceMangaDao.observeAll()
    }

    private suspend fun loadYearlyTopManga(tagId: String) {
        loadState.emit(LoadState.Loading)


        loadState.emit(LoadState.None)
    }

    override suspend fun refresh() = Unit
}