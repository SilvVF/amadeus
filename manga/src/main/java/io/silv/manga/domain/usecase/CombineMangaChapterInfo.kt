package io.silv.manga.domain.usecase

import io.silv.manga.domain.models.DomainChapter
import io.silv.manga.domain.models.DomainManga
import io.silv.manga.domain.repositorys.ChapterInfoRepository
import io.silv.manga.domain.repositorys.MangaRepository
import io.silv.manga.domain.repositorys.SavedMangaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class CombineMangaChapterInfo(
    private val mangaRepository: MangaRepository,
    private val savedMangaRepository: SavedMangaRepository,
    private val chapterInfoRepository: ChapterInfoRepository
) {
    val loading = chapterInfoRepository.loading

    operator fun invoke(id: String): Flow<MangaFull> {
        return combine(
            mangaRepository.getMangaResource(id),
            savedMangaRepository.getSavedManga(id),
            chapterInfoRepository.getChapters(id)
        ) { mangaResource, savedManga, chapterInfo ->
            println("CombinedMangaChapterInfoVolumeImagesRepositoryImpl new MANGA FULL")
            MangaFull(
                domainManga = DomainManga(mangaResource, savedManga),
                volumeImages = savedManga?.volumeToCoverArt ?: mangaResource.volumeToCoverArt,
                chapterInfo = chapterInfo.map { DomainChapter(it) }
            )
        }
    }

    data class MangaFull(
        val domainManga: DomainManga,
        val volumeImages: Map<String, String>?,
        val chapterInfo: List<DomainChapter>?
    )
}