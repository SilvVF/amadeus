package io.silv.manga.domain.repositorys.chapter

import ChapterListResponse
import android.util.Log
import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.domain.ChapterToChapterEntityMapper
import io.silv.manga.domain.coverArtUrl
import io.silv.manga.domain.subtract
import io.silv.manga.domain.suspendRunCatching
import io.silv.manga.domain.timeNow
import io.silv.manga.domain.usecase.GetMangaResourcesById
import io.silv.manga.domain.usecase.UpdateChapterWithArt
import io.silv.manga.domain.usecase.UpdateInfo
import io.silv.manga.domain.usecase.UpdateMangaResourceWithArt
import io.silv.manga.local.dao.ChapterDao
import io.silv.manga.local.dao.SavedMangaDao
import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.local.entity.syncerForEntity
import io.silv.manga.network.mangadex.MangaDexApi
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.toKotlinDuration


internal class OfflineFirstChapterInfoRepository(
    private val chapterDao: ChapterDao,
    private val mangaDexApi: MangaDexApi,
    private val savedMangaDao: SavedMangaDao,
    private val updateChapter: UpdateChapterWithArt,
    private val getMangaResourcesById: GetMangaResourcesById,
    private val updateVolumeWithCoverArt: UpdateMangaResourceWithArt,
    private val dispatchers: AmadeusDispatchers,
): ChapterEntityRepository {

    override val loadingVolumeArtIds = MutableStateFlow(emptyList<String>())

    private val scope = CoroutineScope(dispatchers.io) + CoroutineName("OfflineFirstChapterInfoRepository")

    val syncer = syncerForEntity<ChapterEntity, Chapter, String>(
        networkToKey = { it.id },
        mapper = { n, l ->
            ChapterToChapterEntityMapper.map(n to l)
        },
        upsert = {
            chapterDao.upsertChapter(it)
        }
    )

    private fun Long.lastUpdatedOver(hours: Long): Boolean {
        return Clock.System.now().minus(Instant.fromEpochSeconds(this)).inWholeHours > hours
    }

    private suspend fun shouldUpdate(mangaId: String): Boolean = withContext(dispatchers.io) {
        val chapters = chapterDao.getChaptersByMangaId(mangaId).firstOrNull()?.ifEmpty { null }
        chapters == null || chapters.any { timeNow().subtract(it.savedLocalAt) > java.time.Duration.ofHours(12).toKotlinDuration() }
    }

    override fun getChapters(mangaId: String): Flow<List<ChapterEntity>> {
        return chapterDao.getChaptersByMangaId(mangaId).onStart {
            if (shouldUpdate(mangaId)) {
                Log.d("ChapterEntityRepository","Updating from network")
                updateFromNetwork(mangaId)
                updateVolumeArt(mangaId)
            } else {
                Log.d("ChapterEntityRepository","Skipped Fetch From network")
            }
        }
    }

    private suspend fun getInitialChapterList(mangaId: String, langs: List<String> = listOf("en")) = suspendRunCatching {
        mangaDexApi.getMangaFeed(
            mangaId,
            MangaFeedRequest(
                translatedLanguage = langs,
                offset = 0,
                limit = 500,
                order = mapOf(Order.chapter to OrderBy.asc)
            )
        )
            .getOrThrow()
    }

    private suspend fun getRestOfChapters(response: ChapterListResponse, mangaId: String, langs: List<String> = listOf("en")): List<Chapter> {
        return withContext(dispatchers.io) {
            val count = (response.total / response.limit)
            (1..count).map {
                mangaDexApi.getMangaFeed(
                    mangaId,
                    MangaFeedRequest(
                        translatedLanguage = langs,
                        offset = it * response.limit,
                        limit = 500,
                        order = mapOf(Order.chapter to OrderBy.asc)
                    )
                )
                    .getOrThrow()
                    .data
            }
                .flatten()
        }
    }

    private fun updateFromNetwork(mangaId: String) = scope.launch {
        getInitialChapterList(mangaId)
            .fold(
                onSuccess = {
                    syncer.sync(
                        current = chapterDao.getChaptersByMangaId(mangaId).firstOrNull() ?: emptyList(),
                        networkResponse =  it.data + getRestOfChapters(it, mangaId)
                    )
                },
                onFailure = {

                }
            )
    }

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
                updateVolumeWithCoverArt(id, r, volumeCoverArt + r.volumeToCoverArt)
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

    override suspend fun syncWith(synchronizer: Synchronizer): Boolean {
        val savedManga = savedMangaDao.getSavedManga().first()
        val savedChapters = chapterDao.getChapterEntities().first()
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
