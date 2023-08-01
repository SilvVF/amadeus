package io.silv.manga.domain.repositorys.base

import io.silv.manga.local.entity.MangaResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BasePaginatedRepository<ResourceType: MangaResource, ResourceQuery>(
    private val initialQuery: ResourceQuery,
    protected val MANGA_PAGE_LIMIT: Int = 50
) : PaginatedResourceRepository<ResourceType, ResourceQuery> {

    abstract val scope: CoroutineScope

    override val loadState = MutableStateFlow<LoadState>(LoadState.None)

    protected var currentQuery: ResourceQuery = initialQuery
    private var currentOffset = 0

    private var lastPage = Int.MAX_VALUE

    private val loadPageJobs = mutableListOf<Job>()

    protected fun resetPagination(query: ResourceQuery) {
        loadPageJobs.onEach { it.cancel() }
        loadState.update {
            LoadState.None
        }
        currentOffset = 0
        currentQuery = query
        lastPage = Int.MAX_VALUE
    }

    fun updateLastPage(last: Int) {
        lastPage = last
    }

    protected suspend fun loadPage(
        loadCatching: suspend (offset: Int, query: ResourceQuery) -> Unit
    ) {
        loadPageJobs.add(
            scope.launch {
                val offset = currentOffset
                if (loadState.value != LoadState.None || offset >= lastPage) {
                    return@launch
                }
                loadState.update {
                    if (offset == 0)
                        LoadState.Refreshing
                    else
                        LoadState.Loading
                }
                runCatching {
                    loadCatching(offset, currentQuery)
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
                        loadState.update { LoadState.None }
                    }
            }
        )
    }
}