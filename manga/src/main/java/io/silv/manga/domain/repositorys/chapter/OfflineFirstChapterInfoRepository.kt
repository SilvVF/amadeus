package io.silv.manga.domain.repositorys.chapter

import android.util.Log
import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.ktor_response_mapper.message
import io.silv.ktor_response_mapper.suspendOnFailure
import io.silv.ktor_response_mapper.suspendOnSuccess
import io.silv.manga.domain.coverArtUrl
import io.silv.manga.domain.repositorys.base.LoadState
import io.silv.manga.domain.suspendRunCatching
import io.silv.manga.domain.usecase.GetMangaResourcesById
import io.silv.manga.domain.usecase.UpdateChapterWithArt
import io.silv.manga.domain.usecase.UpdateInfo
import io.silv.manga.domain.usecase.UpdateMangaResourceWithArt
import io.silv.manga.local.dao.ChapterDao
import io.silv.manga.local.dao.SavedMangaDao
import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.ContentRating
import io.silv.manga.network.mangadex.models.chapter.Chapter
import io.silv.manga.network.mangadex.requests.CoverArtRequest
import io.silv.manga.network.mangadex.requests.MangaFeedRequest
import io.silv.manga.network.mangadex.requests.Order
import io.silv.manga.network.mangadex.requests.OrderBy
import io.silv.manga.sync.Synchronizer
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlin.math.ceil
import kotlin.math.roundToInt

data class ChapterInfoResponse(
    val chapters: List<Chapter>,
    val page: Int,
    val lastPage: Int,
    val sortedByAsc: Boolean
)

interface ChapterListRepository {

    val loadingVolumeArtIds: Flow<List<String>>

    fun getChapters(mangaId: String, page: Int, asc: Boolean): Flow<Resource<ChapterInfoResponse>>

    fun getSavedChapters(mangaId: String): Flow<List<ChapterEntity>>
}

internal class ChapterListRepositoryImpl(
    private val chapterDao: ChapterDao,
    private val mangaDexApi: MangaDexApi,
    private val savedMangaDao: SavedMangaDao,
    private val getMangaResourcesById: GetMangaResourcesById,
    private val updateMangaResourceWithArt: UpdateMangaResourceWithArt,
    private val dispatchers: AmadeusDispatchers,
): ChapterListRepository {

    private val scope = CoroutineScope(dispatchers.io) +
            CoroutineName("ChapterInfoRepositoryImpl")

    override fun getSavedChapters(mangaId: String): Flow<List<ChapterEntity>> {
        return chapterDao.getChaptersByMangaId(mangaId).onStart {
            scope.launch {
                Log.d("ChapterInfoRepositoryImpl", "update volume art")
                updateVolumeArt(mangaId).getOrThrow()
            }
        }
    }

    override val loadingVolumeArtIds = MutableStateFlow(emptyList<String>())

    private suspend fun updateVolumeArt(mangaId: String) = suspendRunCatching {

        loadingVolumeArtIds.update { it + mangaId }

        val savedManga = savedMangaDao.getMangaById(mangaId)
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
        .onFailure {
            Log.d("ChapterInfoRepositoryImpl", it.message ?: it.stackTraceToString() )
            it.printStackTrace()
            loadingVolumeArtIds.update { it - mangaId }
        }


    override fun getChapters(
        mangaId: String, page: Int, asc: Boolean
    ): Flow<Resource<ChapterInfoResponse>> = flow {
        emit(Resource.Loading)
        mangaDexApi.getMangaFeed(
            mangaId = mangaId,
            mangaFeedRequest = MangaFeedRequest(
                limit = 96,
                offset = page * 96,
                translatedLanguage = listOf("en"),
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

sealed class Resource<out T> {
    object Loading: Resource<Nothing>()
    data class Success<T>(val result: T): Resource<T>()
    data class Failure<T>(val message: String, val result: T?): Resource<T>()
}

internal class OfflineFirstChapterInfoRepository(
    private val chapterDao: ChapterDao,
    private val mangaDexApi: MangaDexApi,
    private val savedMangaDao: SavedMangaDao,
    private val updateChapter: UpdateChapterWithArt,
    dispatchers: AmadeusDispatchers,
): ChapterInfoRepository {

    private val scope = CoroutineScope(dispatchers.io) +
            CoroutineName("OfflineFirstChapterInfoRepository")

    override val loadState = MutableStateFlow<LoadState>(LoadState.None)

    override fun getChapters(mangaId: String): Flow<List<ChapterEntity>> {
        return chapterDao.getChaptersByMangaId(mangaId)
    }

    override suspend fun syncWith(synchronizer: Synchronizer): Boolean {
        val savedManga = savedMangaDao.getAll()
        val savedChapters = chapterDao.getAll()
        // Delete any chapter that is not associated with a saved manga
        // and that has no chapter images downloaded
        val savedChaptersAfterDeletion = savedChapters.filter { chapter ->
            savedManga.none { it.id == chapter.id } && chapter.chapterImages.isEmpty()
                .also { unused ->
                    if (unused) chapterDao.deleteChapter(chapter)
                }
        }

        return runCatching {
            for (chapterEntity in savedChaptersAfterDeletion) {
                updateChapter(
                    UpdateInfo(
                        id = chapterEntity.mangaId,
                        chapterDao = chapterDao,
                        savedMangaDao = savedMangaDao,
                        mangaDexApi = mangaDexApi,
                        entity = savedManga.find { it.id == chapterEntity.mangaId } ?: continue,
                        page = 0,
                        fetchLatest = true
                    )
                )
            }
        }
            .isSuccess
    }
}
