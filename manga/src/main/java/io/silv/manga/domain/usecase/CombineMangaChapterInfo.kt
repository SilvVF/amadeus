package io.silv.manga.domain.usecase

import io.silv.manga.domain.models.DomainChapter
import io.silv.manga.domain.models.DomainManga
import io.silv.manga.domain.repositorys.ChapterInfoRepository
import io.silv.manga.domain.repositorys.PopularMangaRepository
import io.silv.manga.domain.repositorys.RecentMangaRepository
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.domain.repositorys.SearchMangaRepository
import io.silv.manga.local.entity.MangaResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class CombineMangaChapterInfo(
    private val recentMangaRepository: RecentMangaRepository,
    private val popularMangaRepository: PopularMangaRepository,
    private val searchMangaRepository: SearchMangaRepository,
    private val savedMangaRepository: SavedMangaRepository,
    private val chapterInfoRepository: ChapterInfoRepository
) {
    fun loading(id: String) = chapterInfoRepository.loadingIds
        .map { ids -> ids.any { it == id } }

    operator fun invoke(id: String): Flow<MangaFull> {
        return combine(
            popularMangaRepository.getMangaResource(id),
            searchMangaRepository.getMangaResource(id),
            recentMangaRepository.getMangaResource(id),
            savedMangaRepository.getSavedManga(id),
            chapterInfoRepository.getChapters(id)
        ) { popular, search, recent, savedManga, chapterInfo ->
            println("CombinedMangaChapterInfoVolumeImagesRepositoryImpl new MANGA FULL")
            val resource: MangaResource? = popular ?: search ?: recent
            MangaFull(
                domainManga = resource?.let { DomainManga(it, savedManga) },
                volumeImages = savedManga?.volumeToCoverArt ?: resource?.volumeToCoverArt,
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