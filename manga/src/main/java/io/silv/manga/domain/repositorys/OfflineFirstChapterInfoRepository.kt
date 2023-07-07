package io.silv.manga.domain.repositorys

import io.silv.core.AmadeusDispatchers
import io.silv.core.pForEachKey
import io.silv.ktor_response_mapper.getOrNull
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.ktor_response_mapper.mapSuccess
import io.silv.ktor_response_mapper.suspendOnSuccess
import io.silv.manga.domain.coverArtUrl
import io.silv.manga.local.dao.ChapterDao
import io.silv.manga.local.dao.MangaResourceDao
import io.silv.manga.local.dao.SavedMangaDao
import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.local.entity.ProgressState
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.chapter.Chapter
import io.silv.manga.network.mangadex.models.cover.Cover
import io.silv.manga.network.mangadex.requests.CoverArtRequest
import io.silv.core.Mapper
import io.silv.manga.sync.Synchronizer
import io.silv.manga.sync.syncWithSyncer
import io.silv.manga.local.entity.syncerForEntity
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

private typealias ChapterWithPrevEntity = Pair<Chapter, ChapterEntity?>

internal class OfflineFirstChapterInfoRepository(
    private val chapterDao: ChapterDao,
    private val mangaDexApi: MangaDexApi,
    private val savedMangaDao: SavedMangaDao,
    private val mangaResourceDao: MangaResourceDao,
    dispatchers: AmadeusDispatchers,
): ChapterInfoRepository {

    private val scope = CoroutineScope(dispatchers.io) +
            CoroutineName("OfflineFirstChapterInfoRepository")

    private val mapper = ChapterToChapterEntityMapper()

    private val chapterSyncer = syncerForEntity<ChapterEntity, Chapter, String>(
        networkToKey = { chapter -> chapter.id },
        mapper = { chapter, chapterEntity -> mapper.map(chapter to chapterEntity) },
        upsert = { chapterDao.upsertChapter(it) }
    )

    override val loading = MutableStateFlow(false)

    override fun getChapters(mangaId: String): Flow<List<ChapterEntity>> {
        return chapterDao.getChaptersByMangaId(mangaId).onEach { entities ->
            if (entities.isEmpty()) {
               updateChapterList(mangaId)
            }
        }
    }

    private fun updateChapterList(mangaId: String) = scope.launch {
        loading.emit(true)
        suspend fun getMangaFeed() = mangaDexApi.getMangaFeed(mangaId).mapSuccess {
            data.map { chapter ->
                mapper.map(chapter to null)
            }
        }

        val savedManga = savedMangaDao.getMangaById(mangaId)
        val mangaResource = mangaResourceDao.getMangaById(mangaId)

        if (savedManga == null && mangaResource == null) { return@launch }

        getMangaFeed().getOrNull()?.let { chapterEntities ->
            chapterEntities.forEach { chapterDao.upsertChapter(it) }
            mangaDexApi.getCoverArtList(
                CoverArtRequest(
                    manga = listOf(mangaId),
                    limit = 100,
                   )
            ).suspendOnSuccess {
                if (savedManga != null) {
                    updateMangaCoverArt(
                        mangaId,
                        covers = data.data,
                        manga = savedManga,
                        new = { old, coverList ->
                            old.copy(volumeToCoverArt = old.volumeToCoverArt + coverList)
                        },
                        update = { savedMangaDao.updateSavedManga(it) }
                    )
                } else {
                    updateMangaCoverArt(
                        mangaId,
                        covers = data.data,
                        manga = mangaResource,
                        new = { old, coverList ->
                            old.copy(volumeToCoverArt = old.volumeToCoverArt + coverList)
                        },
                        update = { mangaResourceDao.update(it) }
                    )
                }
            }
        }
    }
        .invokeOnCompletion { loading.tryEmit(false) }


    override suspend fun syncWith(synchronizer: Synchronizer): Boolean {
        val savedChapters = chapterDao.getAll()
        return synchronizer.syncWithSyncer(
            syncer = chapterSyncer,
            getCurrent = { savedChapters },
            getNetwork = {
               savedChapters.groupBy { it.mangaId }
                     .flatMap { (mangaId, _) ->
                         mangaDexApi.getMangaFeed(mangaId)
                             .getOrThrow()
                             .data
                     }
            },
            onComplete = { result ->
                val allChanged = result.added + result.updated
                allChanged
                    .groupBy { it.mangaId }
                    .pForEachKey(scope) { (mangaId) ->
                        mangaDexApi.getCoverArtList(
                            CoverArtRequest(
                                manga = listOf(mangaId),
                                limit = 100,
                            )
                        ).suspendOnSuccess {
                            updateMangaCoverArt(
                                mangaId,
                                covers = data.data,
                                manga = savedMangaDao.getMangaById(mangaId),
                                update = {savedMangaDao.updateSavedManga(it)},
                                new = { entity, covers ->
                                    entity.copy(
                                        volumeToCoverArt = entity.volumeToCoverArt + covers
                                    )
                                }
                            )
                        }
                }
            }
        )
    }

    private suspend fun <ENTITY> updateMangaCoverArt(
        mangaId: String,
        covers: List<Cover>,
        manga: ENTITY?,
        update: suspend (ENTITY) -> Unit,
        new: (ENTITY, Map<String, String>) -> ENTITY
    ) {
       manga?.let { manga ->
           update(
                new(
                    manga,
                    buildMap {
                        covers.forEach {
                            put(
                                it.attributes.volume ?: "0",
                                coverArtUrl(it.attributes.fileName, mangaId)
                            )
                        }
                    }
                )
            )
        }
    }


    private class ChapterToChapterEntityMapper: Mapper<ChapterWithPrevEntity, ChapterEntity> {
        override fun map(from: ChapterWithPrevEntity): ChapterEntity {
            val (chapter, prev) = from
            return ChapterEntity(
                id = chapter.id,
                mangaId = chapter.relationships.find { it.type == "manga" }?.id
                    ?: throw IllegalStateException("Chapter had no related manga id"),
                progressState = prev?.progressState ?: ProgressState.NotStarted,
                volume = chapter.attributes.volume,
                title = chapter.attributes.title ?: "no title",
                pages = chapter.attributes.pages,
                chapterNumber = chapter.attributes.chapter?.toIntOrNull() ?: 0,
                chapterImages = prev?.chapterImages,
                createdAt = chapter.attributes.createdAt,
                updatedAt = chapter.attributes.updatedAt,
            )
        }

    }
}