

package io.silv.data.manga

import io.silv.data.util.Syncable
import io.silv.database.entity.manga.SavedMangaEntity
import io.silv.database.entity.relations.SavedMangaWithChapters
import kotlinx.coroutines.flow.Flow

interface SavedMangaRepository: Syncable {

    suspend fun addOrRemoveFromLibrary(id: String)

    suspend fun saveManga(
        id: String,
        block: ((SavedMangaEntity) -> SavedMangaEntity) = { it }
    ): Boolean

    fun observeSavedMangaListWithChapters(): Flow<List<SavedMangaWithChapters>>

    fun observeSavedMangaWithChaptersById(id: String): Flow<SavedMangaWithChapters?>

    fun observeSavedMangaList(): Flow<List<SavedMangaEntity>>

    fun observeSavedMangaById(id: String): Flow<SavedMangaEntity?>
}
