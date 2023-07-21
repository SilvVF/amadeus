package io.silv.manga.domain.usecase

import io.silv.ktor_response_mapper.getOrNull
import io.silv.manga.domain.ChapterToChapterEntityMapper
import io.silv.manga.domain.coverArtUrl
import io.silv.manga.local.dao.ChapterDao
import io.silv.manga.local.dao.PopularMangaResourceDao
import io.silv.manga.local.dao.RecentMangaResourceDao
import io.silv.manga.local.dao.SavedMangaDao
import io.silv.manga.local.dao.SearchMangaResourceDao
import io.silv.manga.local.entity.MangaResource
import io.silv.manga.local.entity.PopularMangaResource
import io.silv.manga.local.entity.RecentMangaResource
import io.silv.manga.local.entity.SavedMangaEntity
import io.silv.manga.local.entity.SearchMangaResource
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.requests.CoverArtRequest
import io.silv.manga.network.mangadex.requests.MangaFeedRequest

internal data class UpdateInfo(
    val id: String,
    val chapterDao: ChapterDao,
    val savedMangaDao: SavedMangaDao,
    val mangaDexApi: MangaDexApi,
    val entity: SavedMangaEntity
)

internal data class UpdateResourceInfo(
    val id: String,
    val chapterDao: ChapterDao,
    val mangaDexApi: MangaDexApi,
    val daoId: Int,
    val mangaResource: MangaResource
)


internal fun interface UpdateResourceChapterWithArt: suspend  (UpdateResourceInfo) -> Unit {

    companion object {
        fun defaultImpl(
            popularMangaResourceDao: PopularMangaResourceDao,
            recentMangaResourceDao: RecentMangaResourceDao,
            searchMangaResourceDao: SearchMangaResourceDao
        ) = UpdateResourceChapterWithArt { info ->
            updateVolumeCoverArtAndChapterInfoForResource(
                id = info.id,
                chapterDao = info.chapterDao,
                mangaDexApi = info.mangaDexApi,
                update = { volumeToCoverArt ->
                    when(info.daoId) {
                        PopularMangaResourceDao.id -> {
                            popularMangaResourceDao.update(
                                (info.mangaResource as PopularMangaResource).copy(
                                    volumeToCoverArt = volumeToCoverArt
                                )
                            )
                        }
                        RecentMangaResourceDao.id -> {
                            recentMangaResourceDao.update(
                                (info.mangaResource as RecentMangaResource).copy(
                                    volumeToCoverArt = volumeToCoverArt
                                )
                            )
                        }
                        SearchMangaResourceDao.id -> {
                            searchMangaResourceDao.update(
                                (info.mangaResource as SearchMangaResource).copy(
                                    volumeToCoverArt = volumeToCoverArt
                                )
                            )
                        }
                    }
                }
            )
        }
    }
}

internal fun interface UpdateChapterWithArt: suspend  (UpdateInfo) -> Unit {

    companion object {
        fun defaultImpl() = UpdateChapterWithArt { info ->
            updateVolumeCoverArtAndChapterInfo(
                id = info.id,
                chapterDao = info.chapterDao,
                savedMangaDao = info.savedMangaDao,
                mangaDexApi = info.mangaDexApi,
                entity = info.entity
            )
        }
    }
}

private suspend fun getMangaFeedAndUpdateChapter(
    mangaDexApi: MangaDexApi,
    id: String,
    chapterDao: ChapterDao,
) {
    mangaDexApi.getMangaFeed(
        id,
        MangaFeedRequest(
            translatedLanguage = listOf("en"),
            includeEmptyPages = 0,
        )
    )
        .getOrNull()
        ?.let { chapterListResponse ->
            chapterListResponse.data.forEach {
                chapterDao.upsertChapter(
                    ChapterToChapterEntityMapper.map(
                        it to chapterDao.getById(it.id)
                    )
                )
            }
        }
}

// Load chapters info that will also attach volume images
// tries to preload the data and will fail if there is no internet
// this will be fetched later in any screen that needs it or during sync
private suspend fun updateVolumeCoverArtAndChapterInfoForResource(
    id: String,
    chapterDao: ChapterDao,
    update: suspend (Map<String, String>) -> Unit,
    mangaDexApi: MangaDexApi,
) {
    runCatching {
        getMangaFeedAndUpdateChapter(mangaDexApi, id, chapterDao)
        mangaDexApi.getCoverArtList(CoverArtRequest(manga = listOf(id)))
            .getOrNull()?.let { r ->
                update(
                     buildMap {
                         r.data.forEach { cover ->
                             put(
                                 cover.attributes.volume ?: "0",
                                 coverArtUrl(cover.attributes.fileName, id)
                             )
                         }
                     }
                )
            }
    }
}

// Load chapters info that will also attach volume images
// tries to preload the data and will fail if there is no internet
// this will be fetched later in any screen that needs it or during sync
private suspend fun updateVolumeCoverArtAndChapterInfo(
    id: String,
    chapterDao: ChapterDao,
    savedMangaDao: SavedMangaDao,
    mangaDexApi: MangaDexApi,
    entity: SavedMangaEntity
) {
    runCatching {
        getMangaFeedAndUpdateChapter(mangaDexApi, id, chapterDao)
        mangaDexApi.getCoverArtList(CoverArtRequest(manga = listOf(id)))
            .getOrNull()?.let { r ->
                savedMangaDao.updateSavedManga(
                    entity.copy(
                        volumeToCoverArt = buildMap {
                            r.data.forEach { cover ->
                                put(
                                    cover.attributes.volume ?: "0",
                                    coverArtUrl(cover.attributes.fileName, id)
                                )
                            }
                        }
                    )
                )
            }
    }
}