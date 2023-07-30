package io.silv.manga.domain.usecase

import io.silv.core.combine
import io.silv.manga.domain.models.DomainChapter
import io.silv.manga.domain.models.DomainManga
import io.silv.manga.domain.repositorys.ChapterInfoRepository
import io.silv.manga.domain.repositorys.FilteredMangaRepository
import io.silv.manga.domain.repositorys.PopularMangaRepository
import io.silv.manga.domain.repositorys.RecentMangaRepository
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.domain.repositorys.SearchMangaRepository
import io.silv.manga.domain.repositorys.SeasonalMangaRepository
import io.silv.manga.local.entity.MangaResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CombineMangaChapterInfo(
    private val popularMangaRepository: PopularMangaRepository,
    private val recentMangaRepository: RecentMangaRepository,
    private val seasonalMangaRepository: SeasonalMangaRepository,
    private val searchMangaRepository: SearchMangaRepository,
    private val filteredMangaRepository: FilteredMangaRepository,
    private val savedMangaRepository: SavedMangaRepository,
    private val chapterInfoRepository: ChapterInfoRepository,
) {
    fun loading(id: String) = chapterInfoRepository.loadingIds
        .map { ids -> ids.any { it == id } }

    operator fun invoke(id: String): Flow<MangaFull> {
        return combine(
            popularMangaRepository.getMangaResource(id),
            recentMangaRepository.getMangaResource(id),
            seasonalMangaRepository.getSeasonalManga(id),
            searchMangaRepository.getMangaResource(id),
            filteredMangaRepository.getMangaResource(id),
            filteredMangaRepository.getYearlyMangaResource(id),
            savedMangaRepository.getSavedManga(id),
            chapterInfoRepository.getChapters(id),
        ) { popular, recent, seasonal, search, filtered, yearly, saved, chapterInfo ->
            val resource: MangaResource? = popular ?: recent ?: seasonal ?: search ?: filtered ?: yearly
            MangaFull(
                domainManga = resource?.let { DomainManga(it, saved) },
                volumeImages = saved?.volumeToCoverArt ?: resource?.volumeToCoverArt,
                chapterInfo = chapterInfo.map {
                    DomainChapter(it)
                }
            )
        }
    }

    data class MangaFull(
        val domainManga: DomainManga?,
        val volumeImages: Map<String, String>?,
        val chapterInfo: List<DomainChapter>?
    )
}
