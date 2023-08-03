package io.silv.manga.domain.usecase

import io.silv.ktor_response_mapper.getOrNull
import io.silv.manga.domain.ChapterToChapterEntityMapper
import io.silv.manga.domain.coverArtUrl
import io.silv.manga.domain.timeStringMinus
import io.silv.manga.local.dao.ChapterDao
import io.silv.manga.local.dao.FilteredMangaResourceDao
import io.silv.manga.local.dao.FilteredMangaYearlyResourceDao
import io.silv.manga.local.dao.PopularMangaResourceDao
import io.silv.manga.local.dao.RecentMangaResourceDao
import io.silv.manga.local.dao.SavedMangaDao
import io.silv.manga.local.dao.SearchMangaResourceDao
import io.silv.manga.local.dao.SeasonalMangaResourceDao
import io.silv.manga.local.entity.FilteredMangaResource
import io.silv.manga.local.entity.FilteredMangaYearlyResource
import io.silv.manga.local.entity.MangaResource
import io.silv.manga.local.entity.PopularMangaResource
import io.silv.manga.local.entity.RecentMangaResource
import io.silv.manga.local.entity.SavedMangaEntity
import io.silv.manga.local.entity.SearchMangaResource
import io.silv.manga.local.entity.SeasonalMangaResource
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.requests.CoverArtRequest
import io.silv.manga.network.mangadex.requests.MangaFeedRequest
import io.silv.manga.network.mangadex.requests.Order
import io.silv.manga.network.mangadex.requests.OrderBy
import kotlinx.coroutines.flow.first
import java.time.Duration

internal data class UpdateInfo(
    val id: String,
    val chapterDao: ChapterDao,
    val savedMangaDao: SavedMangaDao,
    val mangaDexApi: MangaDexApi,
    val entity: SavedMangaEntity,
    val page: Int,
    val fetchLatest: Boolean = false
)

internal data class UpdateResourceInfo(
    val id: String,
    val chapterDao: ChapterDao,
    val mangaDexApi: MangaDexApi,
    val daoId: Int,
    val mangaResource: MangaResource,
    val page: Int,
)


internal fun interface UpdateResourceChapterWithArt: suspend  (UpdateResourceInfo) -> Unit {

    companion object {
        fun defaultImpl(
            popularMangaResourceDao: PopularMangaResourceDao,
            recentMangaResourceDao: RecentMangaResourceDao,
            searchMangaResourceDao: SearchMangaResourceDao,
            filteredMangaResourceDao: FilteredMangaResourceDao,
            seasonalMangaResourceDao: SeasonalMangaResourceDao,
            filteredMangaYearlyResourceDao: FilteredMangaYearlyResourceDao,
        ) = UpdateResourceChapterWithArt { info ->
            updateVolumeCoverArtAndChapterInfoForResource(
                id = info.id,
                chapterDao = info.chapterDao,
                mangaDexApi = info.mangaDexApi,
                page = info.page,
                update = { volumeToCoverArt ->
                    when(info.daoId) {
                        PopularMangaResourceDao.id -> {
                            popularMangaResourceDao.updatePopularMangaResource(
                                (info.mangaResource as PopularMangaResource).copy(
                                    volumeToCoverArt = volumeToCoverArt
                                )
                            )
                        }
                        RecentMangaResourceDao.id -> {
                            recentMangaResourceDao.updateRecentMangaResource(
                                (info.mangaResource as RecentMangaResource).copy(
                                    volumeToCoverArt = volumeToCoverArt
                                )
                            )
                        }
                        SearchMangaResourceDao.id -> {
                            searchMangaResourceDao.updateSearchMangaResource(
                                (info.mangaResource as SearchMangaResource).copy(
                                    volumeToCoverArt = volumeToCoverArt
                                )
                            )
                        }
                        FilteredMangaResourceDao.id -> {
                            filteredMangaResourceDao.updateFilteredMangaResource(
                                (info.mangaResource as FilteredMangaResource).copy(
                                    volumeToCoverArt = volumeToCoverArt
                                )
                            )
                        }
                        SeasonalMangaResourceDao.id -> {
                            seasonalMangaResourceDao.updateSeasonalMangaResource(
                                (info.mangaResource as SeasonalMangaResource).copy(
                                    volumeToCoverArt = volumeToCoverArt
                                )
                            )
                        }
                        FilteredMangaYearlyResourceDao.id -> {
                            filteredMangaYearlyResourceDao.updateFilteredYearlyMangaResource(
                                (info.mangaResource as FilteredMangaYearlyResource).copy(
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
                entity = info.entity,
                page = info.page,
                fetchLatest = info.fetchLatest
            )
        }
    }
}

private suspend fun getMangaFeedAndUpdateChapter(
    mangaDexApi: MangaDexApi,
    id: String,
    chapterDao: ChapterDao,
    page: Int,
    fetchLatest: Boolean
) {
    mangaDexApi.getMangaFeed(
        id,
        if (fetchLatest) {
            MangaFeedRequest(
                limit = 100,
                translatedLanguage = listOf("en"),
                order = mapOf(Order.createdAt to OrderBy.desc),
                createdAtSince = timeStringMinus(Duration.ofDays(30))
            )
        } else {
            MangaFeedRequest(
                limit = 100,
                offset = page * 100,
                translatedLanguage = listOf("en"),
            )
        }
    )
        .getOrNull()
        ?.let { chapterListResponse ->
            chapterListResponse.data.forEach {
                chapterDao.upsertChapter(
                    ChapterToChapterEntityMapper.map(
                        it to chapterDao.getChapterById(it.id).first()
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
    page: Int,
) {
    runCatching {
        getMangaFeedAndUpdateChapter(mangaDexApi, id, chapterDao, page, false)
        mangaDexApi.getCoverArtList(CoverArtRequest(manga = listOf(id), limit = 100, offset = 0))
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
    entity: SavedMangaEntity,
    page: Int,
    fetchLatest: Boolean,
) {
    runCatching {
        getMangaFeedAndUpdateChapter(mangaDexApi, id, chapterDao, page, fetchLatest)
        mangaDexApi.getCoverArtList(CoverArtRequest(manga = listOf(id),limit = 100, offset = 0))
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