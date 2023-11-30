package io.silv.data.manga

import androidx.paging.Pager
import androidx.paging.PagingConfig
import io.silv.common.time.timeStringMinus
import io.silv.network.MangaDexApi
import io.silv.network.requests.MangaRequest
import kotlin.time.Duration.Companion.days

class QueryPagingSourceRepo(
    private val mangaDexApi: MangaDexApi
) {

    private fun pagingSource(type: PagedType): QueryPagingSource {
        return when (type) {
            PagedType.Latest -> {
                QueryPagingSource(
                    mangaDexApi,
                    MangaRequest(
                        includes = listOf("cover_art", "author", "artist"),
                        availableTranslatedLanguage = listOf("en"),
                        order = mapOf("createdAt" to "desc")
                    )
                )
            }
            PagedType.Popular -> QueryPagingSource(
                mangaDexApi,
                MangaRequest(
                    includes = listOf("cover_art", "author", "artist"),
                    order = mapOf("followedCount" to "desc"),
                    availableTranslatedLanguage = listOf("en"),
                    hasAvailableChapters = true,
                    createdAtSince = timeStringMinus(30.days)
                )
            )
            is PagedType.Query -> QueryPagingSource(mangaDexApi, type.query)
        }
    }

    fun queryPager(config: PagingConfig, type: PagedType) = Pager(
        config = config,
        pagingSourceFactory = {
            pagingSource(type)
        },
    )
}

sealed class PagedType {
    data object Popular: PagedType()
    data object Latest: PagedType()
    data class Query(val query: MangaRequest): PagedType()
}