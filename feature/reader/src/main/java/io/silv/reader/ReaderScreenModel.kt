package io.silv.reader

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.data.download.DownloadManager
import eu.kanade.tachiyomi.reader.loader.ChapterLoader
import eu.kanade.tachiyomi.reader.model.ReaderChapter
import io.silv.domain.chapter.GetSavableChapter
import io.silv.domain.manga.GetSavableManga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReaderScreenModel(
    private val getChapter: GetSavableChapter,
    private val getManga: GetSavableManga,
    private val mangaId: String,
    private val initialChapterId: String,
    private val downloadManager: DownloadManager
): StateScreenModel<ReaderState>(ReaderState.Loading) {

    init {
        screenModelScope.launch {
            init().onFailure { mutableState.value = ReaderState.Error(it.localizedMessage ?: "") }
        }
    }

    private fun updateSuccess(block: (ReaderState.Success) -> ReaderState) =
        mutableState.update { (it as? ReaderState.Success)?.let(block) ?: it }

    private var loader: ChapterLoader? = null

    /**
     * Initializes this presenter with the given [mangaId] and [initialChapterId]. This method will
     * fetch the manga from the database and initialize the initial chapter.
     */
    private suspend fun init(): Result<Unit> = runCatching {
        val chapter = ReaderChapter(
            getChapter.await(initialChapterId)!!
        )
        val manga = getManga.await(mangaId)!!

        loader = ChapterLoader(downloadManager, manga)

        withContext(Dispatchers.IO) {
            loader!!.loadChapter(chapter)
        }

        mutableState.value = ReaderState.Success(
            chapter = chapter
        )
    }
}

sealed interface ReaderState {
    data object Loading: ReaderState
    data class Error(val reason: String): ReaderState
    data class Success(val chapter: ReaderChapter): ReaderState
}
