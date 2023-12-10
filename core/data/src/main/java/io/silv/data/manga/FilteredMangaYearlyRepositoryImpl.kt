package io.silv.data.manga

import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.message
import io.silv.common.AmadeusDispatchers
import io.silv.common.model.Resource
import io.silv.common.model.TagsMode
import io.silv.common.time.timeStringMinus
import io.silv.data.mappers.toSourceManga
import io.silv.database.dao.SourceMangaDao
import io.silv.database.entity.manga.SourceMangaResource
import io.silv.network.MangaDexApi
import io.silv.network.requests.MangaRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.days

internal class FilteredYearlyMangaRepositoryImpl(
    private val sourceMangaDao: SourceMangaDao,
    private val mangaDexApi: MangaDexApi,
    private val dispatchers: AmadeusDispatchers,
): FilteredYearlyMangaRepository {

    private fun topYearlyRequest(tagId: String) =  MangaRequest(
        offset = 0,
        limit = 20,
        includes = listOf("cover_art"),
        availableTranslatedLanguage = listOf("en"),
        hasAvailableChapters = true,
        order = mapOf("followedCount" to "desc"),
        includedTags = listOf(tagId),
        includedTagsMode = TagsMode.AND,
        createdAtSince = timeStringMinus(365.days)
    )

    override fun getYearlyTopMangaByTagId(
        tagId: String
    ) = flow {

        emit(Resource.Loading)

        when(val response = mangaDexApi.getMangaList(topYearlyRequest(tagId))) {
            is ApiResponse.Failure -> {
                emit(Resource.Failure(response.message(), null))
            }
            is ApiResponse.Success -> {
                val data: List<SourceMangaResource> = withContext(dispatchers.io) {
                    response.data.data.map { manga ->
                        manga.toSourceManga()
                    }
                        .also { sourceMangaDao.insertAll(it) }
                }
                emit(Resource.Success(data))
            }
        }
    }
        .flowOn(Dispatchers.IO)
}