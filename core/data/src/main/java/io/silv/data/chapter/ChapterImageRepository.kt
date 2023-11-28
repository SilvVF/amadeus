package io.silv.data.chapter

import io.silv.common.model.Resource
import io.silv.network.model.chapter.Chapter
import kotlinx.coroutines.flow.Flow

/**
 * repository responsible for getting the images for a given manga.
 */
interface ChapterImageRepository {

    suspend fun getChapterImageUrls(chapterId: String, externalUrl: String): Result<List<String>>

    suspend fun getChapterImages(id: String): Flow<Resource<Pair<Chapter, List<String>>>>
}
