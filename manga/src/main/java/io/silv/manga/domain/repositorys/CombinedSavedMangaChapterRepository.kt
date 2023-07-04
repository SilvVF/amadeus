package io.silv.manga.domain.repositorys

import io.silv.manga.domain.models.DomainChapter
import io.silv.manga.domain.models.DomainManga
import io.silv.manga.local.dao.MangaResourceDao
import io.silv.manga.local.dao.SavedMangaDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class MangaFull(
    val domainManga: DomainManga,
    val volumeImages: Map<String, String>?,
    val chapterInfo: List<DomainChapter>?
)

interface CombinedMangaChapterInfoVolumeImagesRepository {

    fun observeManga(mangaId: String): Flow<MangaFull>
}

internal class CombinedMangaChapterInfoVolumeImagesRepositoryImpl(
    private val savedMangaDao: SavedMangaDao,
    private val mangaResourceDao: MangaResourceDao,
    private val chapterInfoRepository: ChapterInfoRepository
): CombinedMangaChapterInfoVolumeImagesRepository {

    override fun observeManga(mangaId: String): Flow<MangaFull> {
        return combine(
                mangaResourceDao.getResourceAsFlowById(mangaId),
                savedMangaDao.getMangaByIdAsFlow(mangaId),
                chapterInfoRepository.getChapters(mangaId)
            ) { mangaResource, savedManga, chapterInfo ->
                println("CombinedMangaChapterInfoVolumeImagesRepositoryImpl new MANGA FULL")
                MangaFull(
                    domainManga = DomainManga(mangaResource, savedManga),
                    volumeImages = savedManga?.volumeToCoverArt ?: mangaResource.volumeToCoverArt,
                    chapterInfo = chapterInfo.map { DomainChapter(it) }
                )
            }
    }
}