package io.silv.manga.domain.repositorys.chapter

import io.silv.manga.domain.repositorys.base.Resource
import io.silv.manga.network.mangadex.models.chapter.Chapter
import kotlinx.coroutines.flow.Flow

interface ChapterListRepository {

    val loadingVolumeArtIds: Flow<List<String>>

    fun observeChapters(mangaId: String, page: Int, asc: Boolean): Flow<Resource<ChapterInfoResponse>>

    suspend fun loadVolumeArt(mangaId: String)
}

data class ChapterInfoResponse(
    val chapters: List<Chapter>,
    val page: Int,
    val lastPage: Int,
    val sortedByAsc: Boolean
)