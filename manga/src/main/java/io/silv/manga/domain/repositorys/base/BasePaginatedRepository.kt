package io.silv.manga.domain.repositorys.base

import io.silv.manga.local.entity.MangaResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BasePaginatedRepository<ResourceType: MangaResource, ResourceQuery>(
    private val initialQuery: ResourceQuery,
    protected val pageSize: Int = 50
) : PaginatedResourceRepository<ResourceType, ResourceQuery> {

    abstract val scope: CoroutineScope

    override fun latestQuery(): ResourceQuery = query

    override val loadState = MutableStateFlow<PagedLoadState>(PagedLoadState.None)

    private var query: ResourceQuery = initialQuery
    private var offset = 0
    private var lastPage = Int.MAX_VALUE
    private val loadPageJobs = mutableListOf<Job>()

    protected fun resetPagination(query: ResourceQuery) {
        loadPageJobs.onEach { it.cancel() }
        loadState.update {
            PagedLoadState.None
        }
        offset = 0
        this.query = query
        lastPage = Int.MAX_VALUE
    }

    override suspend fun refresh(resourceQuery: ResourceQuery?) {
        resetPagination(resourceQuery ?: initialQuery)
        loadNextPage()
    }

    fun updateLastPage(last: Int) {
        lastPage = last
    }

    protected suspend fun loadPage(
        loadCatching: suspend (offset: Int, query: ResourceQuery) -> Unit
    ) {
        loadPageJobs.add(
            scope.launch {
                if (loadState.value != PagedLoadState.None || offset >= lastPage) {
                    return@launch
                }
                loadState.update {
                    if (offset == 0)
                        PagedLoadState.Refreshing
                    else
                        PagedLoadState.Loading
                }
                runCatching {
                    loadCatching(offset, query)
                }
                    .onSuccess {
                        offset += pageSize
                        loadState.update {
                            if (offset >= lastPage) {
                                PagedLoadState.End
                            } else {
                                PagedLoadState.None
                            }
                        }
                    }
                    .onFailure { throwable ->
                        loadState.update { PagedLoadState.Error(throwable) }
                    }
            }
        )
    }
}
