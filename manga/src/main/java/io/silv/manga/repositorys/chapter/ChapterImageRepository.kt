package io.silv.manga.repositorys.chapter

import io.silv.manga.network.mangadex.models.chapter.Chapter
import io.silv.manga.repositorys.Resource
import kotlinx.coroutines.flow.Flow

/**
 * repository responsible for getting the images for a given manga.
 */
interface ChapterImageRepository {

    suspend fun getChapterImageUrls(chapterId: String, externalUrl: String): Result<List<String>>

    suspend fun getChapterImages(id: String): Flow<Resource<Pair<Chapter, List<String>>>>
}
