package io.silv.manga.domain.repositorys

import io.silv.manga.domain.models.mapToDomainManga
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class CombinedResourceSavedMangaRepository(
    private val mangaRepository: MangaRepository,
    private val savedMangaRepository: SavedMangaRepository
) {

    fun observeAll(
        mangaQuery: MangaQuery
    ) = savedMangaRepository.getSavedManga()
        .combine(
            mangaRepository.getMagnaResources(mangaQuery)
        ) { savedManga, mangaResource ->
            mangaResource.map { resource ->
                resource.mapToDomainManga(
                    savedManga = savedManga.find { resource.id == it.id }
                )
            }
        }

    fun observeAllBookmarked() =
        savedMangaRepository.getSavedManga().map { saved ->
            saved.filter { manga ->
                manga.bookmarked
            }
    }

}