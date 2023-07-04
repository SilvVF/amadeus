package io.silv.manga.domain.repositorys

import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.ktor_response_mapper.suspendOnSuccess
import io.silv.manga.domain.models.DomainChapter
import io.silv.manga.local.dao.ChapterDao
import io.silv.manga.local.dao.MangaResourceDao
import io.silv.manga.local.dao.SavedMangaDao
import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.local.entity.ProgressState
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.chapter.Chapter
import io.silv.manga.network.mangadex.models.cover.Cover
import io.silv.manga.network.mangadex.requests.CoverArtRequest
import io.silv.manga.sync.Mapper
import io.silv.manga.sync.Synchronizer
import io.silv.manga.sync.syncWithSyncer
import io.silv.manga.sync.syncerForEntity
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
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

    override fun getChapters(mangaId: String): Flow<List<ChapterEntity>> {
        return chapterDao.getChaptersByMangaId(mangaId)
    }

    override suspend fun syncWith(synchronizer: Synchronizer, params: String?): Boolean {
        return synchronizer.syncWithSyncer(
            syncer = chapterSyncer,
            getCurrent = { chapterDao.getAll() },
            getNetwork = {
                mangaDexApi.getMangaFeed(
                    params ?: throw IllegalArgumentException()
                )
                    .getOrThrow()
                    .data
            },
            onComplete = { result ->
                result.added.groupBy { it.mangaId }.forEach { (mangaId) ->
                    scope.launch {
                        mangaDexApi.getCoverArtList(
                            CoverArtRequest(
                                manga = listOf(mangaId),
                                limit = 100,
                            )
                        )
                            .suspendOnSuccess {
                                updateSavedMangaCoverArt(mangaId, data.data)
                        }
                    }
                }
            }
        )
    }

    private suspend fun updateSavedMangaCoverArt(mangaId: String, covers: List<Cover>) {
        fun getImgUrl(fileName: String) = "https://uploads.mangadex.org/covers/$mangaId/${fileName}"
        savedMangaDao.getMangaById(mangaId)?.let { manga ->
            savedMangaDao.updateSavedManga(
                manga.copy(
                    volumeToCoverArt = buildMap {
                        putAll(manga.volumeToCoverArt)
                        covers.forEach {
                            put(it.attributes.volume ?: "null", getImgUrl(it.attributes.fileName))
                        }
                    }
                )
            )
        } ?:
        mangaResourceDao.getMangaById(mangaId)?.let { manga ->
            mangaResourceDao.update(
                manga.copy(
                    volumeToCoverArt = buildMap {
                        putAll(manga.volumeToCoverArt)
                        covers.forEach {
                            put(it.attributes.volume ?: "null", getImgUrl(it.attributes.fileName))
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