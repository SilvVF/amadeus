package io.silv.manga.domain.usecase

import io.silv.manga.domain.models.DomainChapter
import io.silv.manga.domain.models.DomainManga
import io.silv.manga.domain.repositorys.ChapterInfoRepository
import io.silv.manga.domain.repositorys.MangaRepository
import io.silv.manga.domain.repositorys.SavedMangaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class CombineMangaChapterInfo(
    private val mangaRepository: MangaRepository,
    private val savedMangaRepository: SavedMangaRepository,
    private val chapterInfoRepository: ChapterInfoRepository
) {
    fun loading(id: String) = chapterInfoRepository.loadingIds
        .map { ids -> ids.any { it == id } }

    operator fun invoke(id: String): Flow<MangaFull> {
        return combine(
            mangaRepository.getMangaResource(id),
            savedMangaRepository.getSavedManga(id),
            chapterInfoRepository.getChapters(id)
        ) { mangaResource, savedManga, chapterInfo ->
            println("CombinedMangaChapterInfoVolumeImagesRepositoryImpl new MANGA FULL")
            MangaFull(
                domainManga = mangaResource?.let { DomainManga(it, savedManga) },
                volumeImages = savedManga?.volumeToCoverArt ?: mangaResource?.volumeToCoverArt,
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