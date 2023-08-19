

package io.silv.manga.repositorys.manga

import io.silv.manga.local.entity.SavedMangaEntity
import io.silv.manga.local.entity.relations.SavedMangaWithChapters
import io.silv.manga.sync.Syncable
import kotlinx.coroutines.flow.Flow

interface SavedMangaRepository: Syncable {

    suspend fun bookmarkManga(id: String)

    suspend fun saveManga(id: String, copy: ((SavedMangaEntity) -> SavedMangaEntity)? = null)

    fun getSavedMangaWithChapters(): Flow<List<SavedMangaWithChapters>>

    fun getSavedMangaWithChapter(id: String): Flow<SavedMangaWithChapters?>

    fun getSavedMangas(): Flow<List<SavedMangaEntity>>

    fun getSavedManga(id: String): Flow<SavedMangaEntity?>
}
