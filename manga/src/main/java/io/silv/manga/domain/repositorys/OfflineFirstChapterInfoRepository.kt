package io.silv.manga.domain.repositorys

import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.ktor_response_mapper.suspendOnSuccess
import io.silv.manga.domain.models.DomainChapter
import io.silv.manga.local.dao.ChapterDao
import io.silv.manga.local.dao.SavedMangaDao
import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.local.entity.ProgressState
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.chapter.Chapter
import io.silv.manga.network.mangadex.models.cover.Cover
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
    private val savedMangaRepository: SavedMangaDao,
    private val dispatchers: AmadeusDispatchers,
): ChapterInfoRepository {

    private val scope = CoroutineScope(dispatchers.io) + CoroutineName("OfflineFirstChapterInfoRepository")
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
                result.added.groupBy { it.mangaId }.forEach { (mangaId, chapterList) ->
                    scope.launch {
                        mangaDexApi.getCoverArtById(mangaId)
                            .suspendOnSuccess {
                                val volume = chapterList.firstOrNull()?.volume ?: "0"
                                updateSavedMangaCoverArt(mangaId, data, volume)
                        }
                    }
                }
            }
        )
    }

    private suspend fun updateSavedMangaCoverArt(mangaId: String, cover: Cover, volume: String) {
        savedMangaRepository.getMangaById(mangaId)?.let { manga ->
            val coverArtUrl = "https://uploads.mangadex.org/covers/$mangaId/${cover.attributes.fileName}"
            savedMangaRepository.updateSavedManga(
                manga.copy(
                    volumeToCoverArt = buildMap {
                        putAll(manga.volumeToCoverArt)
                        put(volume, coverArtUrl)
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