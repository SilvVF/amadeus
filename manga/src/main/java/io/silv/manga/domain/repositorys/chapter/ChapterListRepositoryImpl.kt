package io.silv.manga.domain.repositorys.chapter

import android.util.Log
import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.ktor_response_mapper.message
import io.silv.ktor_response_mapper.suspendOnFailure
import io.silv.ktor_response_mapper.suspendOnSuccess
import io.silv.manga.domain.coverArtUrl
import io.silv.manga.domain.repositorys.base.Resource
import io.silv.manga.domain.suspendRunCatching
import io.silv.manga.domain.usecase.GetMangaResourcesById
import io.silv.manga.domain.usecase.UpdateMangaResourceWithArt
import io.silv.manga.local.dao.SavedMangaDao
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.ContentRating
import io.silv.manga.network.mangadex.requests.CoverArtRequest
import io.silv.manga.network.mangadex.requests.MangaFeedRequest
import io.silv.manga.network.mangadex.requests.Order
import io.silv.manga.network.mangadex.requests.OrderBy
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.roundToInt

internal class ChapterListRepositoryImpl(
    private val mangaDexApi: MangaDexApi,
    private val savedMangaDao: SavedMangaDao,
    private val getMangaResourcesById: GetMangaResourcesById,
    private val updateMangaResourceWithArt: UpdateMangaResourceWithArt,
    private val dispatchers: AmadeusDispatchers,
): ChapterListRepository {

    private val scope = CoroutineScope(dispatchers.io) +
            CoroutineName("ChapterInfoRepositoryImpl")

    override suspend fun loadVolumeArt(mangaId: String) {
        Log.d("ChapterInfoRepositoryImpl", "update volume art")
        updateVolumeArt(mangaId)
    }

    override val loadingVolumeArtIds = MutableStateFlow(emptyList<String>())

    private suspend fun updateVolumeArt(mangaId: String) = withContext(scope.coroutineContext) {
        suspendRunCatching {

            loadingVolumeArtIds.update { it + mangaId }

            val savedManga = savedMangaDao.getSavedMangaById(mangaId).first()
            val resourceToIdList = getMangaResourcesById(mangaId).also {
                Log.d("ChapterInfoRepositoryImpl", it.size.toString() + "resources")
            }

            val list = mangaDexApi.getCoverArtList(
                CoverArtRequest(
                    manga = listOf(mangaId),
                    limit = 100,
                    offset = 0
                )
            )
                .getOrThrow()

            val volumeCoverArt = buildMap {
                list.data.forEach { cover ->
                    put(
                        cover.attributes.volume ?: "0",
                        coverArtUrl(cover.attributes.fileName, mangaId)
                    )
                }
            }

            if (savedManga != null) {
                savedMangaDao.updateSavedManga(
                    savedManga.copy(
                        volumeToCoverArt = savedManga.volumeToCoverArt + volumeCoverArt
                    )
                )
            }
            resourceToIdList.forEach { (r, id) ->
                Log.d("ChapterInfoRepositoryImpl", id.toString() + "trying to update")
                updateMangaResourceWithArt(id, r, volumeCoverArt + r.volumeToCoverArt)
            }
        }
            .onSuccess {
                loadingVolumeArtIds.update { it - mangaId }
            }
            .onFailure { e ->
                Log.d("ChapterInfoRepositoryImpl", e.message ?: e.stackTraceToString())
                loadingVolumeArtIds.update { it - mangaId }
            }
    }

    override fun observeChapters(
        mangaId: String, page: Int, asc: Boolean, languages: List<String>
    ): Flow<Resource<ChapterInfoResponse>> = flow {
        emit(Resource.Loading)
        mangaDexApi.getMangaFeed(
            mangaId = mangaId,
            mangaFeedRequest = MangaFeedRequest(
                limit = 96,
                offset = page * 96,
                includes = listOf("scanlation_group", "user"),
                translatedLanguage = languages.ifEmpty { null },
                order = if (asc) mapOf(Order.volume to  OrderBy.asc) else mapOf(Order.volume to  OrderBy.desc),
                contentRating = listOf(ContentRating.safe, ContentRating.suggestive, ContentRating.pornographic, ContentRating.erotica)
            )
        )
            .suspendOnSuccess {
                emit(
                    Resource.Success(
                        ChapterInfoResponse(
                            chapters = data.data,
                            page = page,
                            lastPage = ceil(data.total / 96f).roundToInt(),
                            sortedByAsc = asc
                        )
                    )
                )
            }
            .suspendOnFailure {
                emit(Resource.Failure(message(), null))
            }
    }
        .flowOn(dispatchers.io)
}
