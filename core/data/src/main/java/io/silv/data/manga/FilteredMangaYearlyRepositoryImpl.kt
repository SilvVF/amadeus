package io.silv.data.manga

import android.util.Log
import androidx.room.withTransaction
import io.silv.common.AmadeusDispatchers
import io.silv.common.model.Resource
import io.silv.common.time.timeStringMinus
import io.silv.data.mappers.toSourceManga
import io.silv.database.AmadeusDatabase
import io.silv.database.entity.manga.remotekeys.YearlyTopKey
import io.silv.ktor_response_mapper.ApiResponse
import io.silv.ktor_response_mapper.message
import io.silv.network.MangaDexApi
import io.silv.network.requests.MangaRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlin.time.Duration.Companion.days

internal class FilteredYearlyMangaRepositoryImpl(
    private val db: AmadeusDatabase,
    private val mangaDexApi: MangaDexApi,
    dispatchers: AmadeusDispatchers,
): FilteredYearlyMangaRepository {

    private val sourceMangaDao = db.sourceMangaDao()
    private val keyDao = db.yearlyTopKeyDao()

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    private fun topYearlyRequest(tagId: String) =  MangaRequest(
        offset = 0,
        limit = 20,
        includes = listOf("cover_art"),
        availableTranslatedLanguage = listOf("en"),
        hasAvailableChapters = true,
        order = mapOf("followedCount" to "desc"),
        includedTags = listOf(tagId),
        includedTagsMode = MangaRequest.TagsMode.AND,
        createdAtSince = timeStringMinus(365.days)
    )

    override fun collectYearlyTopByTagId(
        tagId: String
    ) = channelFlow {

        send(Resource.Loading)

        val response = mangaDexApi.getMangaList(
            topYearlyRequest(tagId)
        )

        when(response) {
            is ApiResponse.Failure -> {
                send(
                    Resource.Failure(response.message(), null)
                )
            }
            is ApiResponse.Success -> {
                db.withTransaction {

                    response.data.data.forEachIndexed { i, manga ->

                        sourceMangaDao.insert(manga.toSourceManga())

                        val prev = keyDao.getByMangaId(manga.id)
                        keyDao.insert(
                            YearlyTopKey(
                                mangaId = manga.id,
                                tagIds = buildSet {
                                    prev?.let { addAll(it.tagIds) }
                                    add(tagId)
                                }.toList(),
                                tagIdToPlacement = buildMap {
                                    prev?.tagIdToPlacement?.let {
                                        putAll(it)
                                    }
                                    put(tagId, i)
                                }
                            )
                        )
                    }

                    keyDao.observeTopYearlyByTagId(tagId)
                        .collect {
                            Log.d("Yearly", "emitting items count: ${it.size}")
                            send(Resource.Success(it.map { it.manga }))
                        }
                }
            }
        }
    }
        .flowOn(Dispatchers.IO)

    override suspend fun refresh() = Unit
}