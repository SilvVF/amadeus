package io.silv.manga.domain.repositorys

import io.silv.manga.domain.models.mapToDomainManga
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class CombinedResourceSavedMangaRepository(
    private val mangaRepository: MangaRepository,
    private val savedMangaRepository: SavedMangaRepository
) {

    suspend fun loadNextPage() = mangaRepository.loadNextPage()

    fun observeAll(
        mangaQuery: MangaQuery
    ) = savedMangaRepository.getSavedMangas()
        .combine(
            mangaRepository.getMangaResources(mangaQuery)
        ) { savedManga, mangaResource ->
            mangaResource.map { resource ->
                resource.mapToDomainManga(
                    savedManga = savedManga.find { resource.id == it.id }
                )
            }
        }

    fun observeAllBookmarked() =
        savedMangaRepository.getSavedMangas().map { saved ->
            saved.filter { manga ->
                manga.bookmarked
            }
    }

}