package io.silv.data.manga

import androidx.paging.Pager
import androidx.paging.PagingConfig
import io.silv.common.model.PagedType
import io.silv.common.model.QueryFilters
import io.silv.common.model.TagsMode
import io.silv.common.model.TimePeriod
import io.silv.common.time.timeStringMinus
import io.silv.common.time.toMangaDexTimeString
import io.silv.data.mappers.timeString
import io.silv.domain.manga.MangaPagingSourceFactory
import io.silv.domain.manga.repository.MangaRepository
import io.silv.network.MangaDexApi
import io.silv.network.requests.MangaRequest
import kotlin.time.Duration.Companion.days


internal class MangaPagingSourceFactoryImpl(
    private val mangaRepository: MangaRepository,
    private val mangaDexApi: MangaDexApi,
): MangaPagingSourceFactory {

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

    private fun timePeriodsMangaRequest(tagId: String, timePeriod: TimePeriod) =  MangaRequest(
        includes = listOf("cover_art", "author", "artist"),
        order = mapOf("followedCount" to "desc"),
        availableTranslatedLanguage = listOf("en"),
        includedTags = listOf(tagId),
        includedTagsMode = TagsMode.OR,
        hasAvailableChapters = true,
        createdAtSince = timePeriod.timeString()
    )

    private fun queryMangaRequest(
        filters: QueryFilters
    ) =  MangaRequest(
        includes = listOf("cover_art", "author", "artist"),
        order = filters.order,
        includedTags = filters.includedTags,
        availableTranslatedLanguage = listOf("en"),
        hasAvailableChapters = filters.hasAvailableChapters,
        createdAtSince = filters.createdAtSince?.toMangaDexTimeString(),
        includedTagsMode = filters.includedTagsMode,
        excludedTagsMode = filters.excludedTagsMode,
        artists = filters.artists,
        authors = filters.authors,
        title = filters.title,
        year = filters.year,
        excludedTags = filters.excludedTags,
        status = filters.status,
        originalLanguage = filters.originalLanguage,
        excludedOriginalLanguage = filters.excludedOriginalLanguage,
        publicationDemographic = filters.publicationDemographic,
        ids = filters.ids,
        contentRating = filters.contentRating,
        updatedAtSince = filters.updatedAtSince?.toMangaDexTimeString(),
        group = filters.group
    )

    override fun pager(config: PagingConfig, type: PagedType) = Pager(
        config = config,
        pagingSourceFactory = {
           QueryPagingSource(
               mangaDexApi,
               when (type) {
                   PagedType.Latest -> latestMangaRequest
                   PagedType.Popular -> popularMangaRequest
                   is PagedType.Query -> queryMangaRequest(type.filters)
                   is PagedType.TimePeriod -> timePeriodsMangaRequest(type.tagId, type.timePeriod)
               },
               mangaRepository = mangaRepository
           )
        },
    )
}
