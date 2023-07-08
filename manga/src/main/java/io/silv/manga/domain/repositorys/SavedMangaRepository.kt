

package io.silv.manga.domain.repositorys

import io.silv.manga.domain.models.DomainManga
import io.silv.manga.local.entity.SavedMangaEntity
import io.silv.manga.local.entity.relations.MangaWithChapters
import io.silv.manga.sync.Syncable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

interface SavedMangaRepository: Syncable {

    suspend fun bookmarkManga(id: String)

    suspend fun saveManga(id: String)

    fun getSavedMangaWithChapters(): Flow<List<MangaWithChapters>>

    fun getSavedMangaWithChapter(id: String): Flow<MangaWithChapters?>

    fun getSavedMangas(): Flow<List<SavedMangaEntity>>

    fun getSavedManga(id: String): Flow<SavedMangaEntity?>
}
