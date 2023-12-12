

package io.silv.data.manga

import io.silv.data.util.Syncable
import io.silv.database.entity.manga.SavedMangaEntity
import io.silv.database.entity.relations.SavedMangaWithChapters
import kotlinx.coroutines.flow.Flow

interface SavedMangaRepository: Syncable {

    suspend fun addMangaToLibrary(id: String)

    suspend fun saveManga(
        id: String,
        block: ((SavedMangaEntity) -> SavedMangaEntity) = { it }
    ): Boolean

    fun getSavedMangaWithChapters(): Flow<List<SavedMangaWithChapters>>

    fun getSavedMangaWithChapter(id: String): Flow<SavedMangaWithChapters?>

    fun getSavedMangas(): Flow<List<SavedMangaEntity>>

    fun getSavedManga(id: String): Flow<SavedMangaEntity?>
}
