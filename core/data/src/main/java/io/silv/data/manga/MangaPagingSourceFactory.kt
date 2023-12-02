package io.silv.data.manga

import androidx.paging.Pager
import androidx.paging.PagingConfig
import io.silv.common.model.PagedType
import io.silv.common.model.QueryFilters
import io.silv.common.time.timeStringMinus
import io.silv.data.mappers.timeString
import io.silv.database.AmadeusDatabase
import io.silv.network.MangaDexApi
import io.silv.network.requests.MangaRequest
import kotlin.time.Duration.Companion.days


class MangaPagingSourceFactory(
    private val db: AmadeusDatabase,
    private val mangaDexApi: MangaDexApi,
) {

    private val latestMangaRequest =  MangaRequest(
        includes = listOf("cover_art", "author", "artist"),
        availableTranslatedLanguage = listOf("en"),
        order = mapOf("createdAt" to "desc")
    )

    private val popularMangaRequest =  MangaRequest(
        includes = listOf("cover_art", "author", "artist"),
        order = mapOf("followedCount" to "desc"),
        availableTranslatedLanguage = listOf("en"),
        hasAvailableChapters = true,
        createdAtSince = timeStringMinus(30.days)
    )
    private fun queryMangaRequest(
        filters: QueryFilters
    ) =  MangaRequest(
        includes = listOf("cover_art", "author", "artist"),
        order = mapOf("followedCount" to "desc"),
        includedTags = filters.tagId?.let { listOf(it) },
        availableTranslatedLanguage = listOf("en"),
        hasAvailableChapters = true,
        createdAtSince = filters.timePeriod.timeString()
    )

    fun pager(config: PagingConfig, type: PagedType) = Pager(
        config = config,
        pagingSourceFactory = {
           QueryPagingSource(
               mangaDexApi,
               when (type) {
                   PagedType.Latest -> latestMangaRequest
                   PagedType.Popular -> popularMangaRequest
                   is PagedType.Query -> queryMangaRequest(type.filters)
               },
               db.sourceMangaDao()
           )
        },
    )

    companion object {
        const val LATEST_MANGA_QUERY =  "io.silv.query.latest"
        const val POPULAR_MANGA_QUERY =  "io.silv.query.popular"
        const val FILTER_MANGA_QUERY = "io.silv.query.filter"
        const val TIME_PERIOD_MANGA_QUERY = "io.silv.query.timeperiod"
    }
}
