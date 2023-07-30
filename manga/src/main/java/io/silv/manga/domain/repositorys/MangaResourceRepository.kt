package io.silv.manga.domain.repositorys

import io.silv.manga.local.entity.MangaResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface MangaResourceRepository<ResourceType: MangaResource> {

    fun getMangaResource(id: String): Flow<ResourceType?>

    fun getMangaResources(): Flow<List<ResourceType>>

    suspend fun loadNextPage()

    val loadState: Flow<LoadState>
}

abstract class PaginatedResourceRepo<ResourceType: MangaResource>() : MangaResourceRepository<ResourceType> {

    private val scope = CoroutineScope(Dispatchers.IO)

    override val loadState = MutableStateFlow<LoadState>(LoadState.None)

    private var currentOffset = 0
    protected val MANGA_PAGE_LIMIT = 50
    private var lastPage = Int.MAX_VALUE

    private var loadPageJobs = mutableListOf<Job>()

    protected fun resetPagination() {
        loadPageJobs.onEach { it.cancel() }
        loadState.update {
            LoadState.None
        }
        currentOffset = 0
        lastPage = Int.MAX_VALUE
    }

    protected suspend fun <T> loadPage(
        params: T,
        loadCatching: suspend (offset: Int, T) -> Unit
    ) {
        loadPageJobs.add(
            scope.launch {
                val offset = currentOffset
                if (offset >= lastPage || loadState.value != LoadState.None) {
                    return@launch
                }
                loadState.update {
                    if (offset == 0)
                        LoadState.Refreshing
                    else
                        LoadState.Loading
                }
                runCatching {
                    loadCatching(offset, params)
                }
                    .onSuccess {
                        currentOffset = offset + MANGA_PAGE_LIMIT
                        loadState.update {
                            if (offset + MANGA_PAGE_LIMIT >= lastPage) {
                                LoadState.End
                            } else {
                                LoadState.None
                            }
                        }
                    }
                    .onFailure {
                        loadState.emit(LoadState.None)
                    }
            }
        )
    }

    fun updateLastPage(last: Int) {
        lastPage = last
    }

    protected suspend fun loadPage(
        loadCatching: suspend (offset: Int) -> Unit
    ) {
        loadPageJobs.add(
            scope.launch {
                val offset = currentOffset
                if (loadState.value != LoadState.None || offset >= lastPage) {
                    return@launch
                }
                loadState.emit(
                    if (offset == 0)
                        LoadState.Refreshing
                    else
                        LoadState.Loading
                )
                runCatching {
                    loadCatching(offset)
                }
                    .onSuccess {
                        currentOffset = offset + MANGA_PAGE_LIMIT
                        loadState.emit(
                            if (offset + MANGA_PAGE_LIMIT >= lastPage) {
                                LoadState.End
                            } else {
                                LoadState.None
                            }
                        )
                    }
                    .onFailure {
                        loadState.emit(LoadState.None)
                    }
            }
        )
    }
}