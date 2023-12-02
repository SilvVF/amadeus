package io.silv.data.manga

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import io.silv.common.model.PagedType
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

    private fun createMediator(query: String, mangaRequest: MangaRequest) = SourceMangaRemoteMediator(
        db = db,
        mangaDexApi = mangaDexApi,
        query = query,
        mangaRequest = mangaRequest
    )


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

    private val remoteKeysDao = db.remoteKeyDao()

    @OptIn(ExperimentalPagingApi::class)
    fun pager(type: PagedType, config: PagingConfig) = Pager(
            config = config,
            remoteMediator = when (type) {
                PagedType.Latest -> createMediator(LATEST_MANGA_QUERY, latestMangaRequest)
                PagedType.Popular -> createMediator(POPULAR_MANGA_QUERY, popularMangaRequest)
                is PagedType.Query -> createMediator(FILTER_MANGA_QUERY, MangaRequest(title = type.filters.query))
                is PagedType.Period -> createMediator(
                    TIME_PERIOD_MANGA_QUERY,
                    MangaRequest(
                        includes = listOf("cover_art", "author", "artist"),
                        availableTranslatedLanguage = listOf("en"),
                        hasAvailableChapters = true,
                        order = mapOf("followedCount" to "desc"),
                        includedTags = listOf(type.tagId),
                        includedTagsMode = MangaRequest.TagsMode.AND,
                        createdAtSince = type.timePeriod.timeString(),
                    )
                )
            },
            pagingSourceFactory = {
               remoteKeysDao.getPagingSourceForQuery(
                    when (type) {
                        PagedType.Latest -> LATEST_MANGA_QUERY
                        PagedType.Popular ->  POPULAR_MANGA_QUERY
                        is PagedType.Query -> FILTER_MANGA_QUERY
                        is PagedType.Period -> TIME_PERIOD_MANGA_QUERY
                    }
                )
            }
        )


    private fun memoryPagingSource(type: PagedType): QueryPagingSource {
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
            is PagedType.Query -> QueryPagingSource(mangaDexApi, MangaRequest(title = type.filters.query))
            is PagedType.Period -> QueryPagingSource(mangaDexApi,  MangaRequest(
                includes = listOf("cover_art", "author", "artist"),
                availableTranslatedLanguage = listOf("en"),
                hasAvailableChapters = true,
                order = mapOf("followedCount" to "desc"),
                includedTags = listOf(type.tagId),
                includedTagsMode = MangaRequest.TagsMode.AND,
                createdAtSince = type.timePeriod.timeString(),
                contentRating = listOf("safe")
            ))
        }
    }

    fun memoryQueryPager(config: PagingConfig, type: PagedType) = Pager(
        config = config,
        pagingSourceFactory = {
            memoryPagingSource(type)
        },
    )

    companion object {
        const val LATEST_MANGA_QUERY =  "io.silv.query.latest"
        const val POPULAR_MANGA_QUERY =  "io.silv.query.popular"
        const val FILTER_MANGA_QUERY = "io.silv.query.filter"
        const val TIME_PERIOD_MANGA_QUERY = "io.silv.query.timeperiod"
    }
}
