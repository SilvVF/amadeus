package io.silv.amadeus.ui.screens.manga_reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import cafe.adriel.voyager.core.model.coroutineScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.local.workers.ChapterDownloadWorker
import kotlinx.coroutines.guava.asDeferred
import kotlinx.coroutines.launch
import java.util.UUID

class MangaReaderSM(
    private val workManager: WorkManager,
): AmadeusScreenModel<MangaReaderEvent>() {

    fun loadMangaImages(
        chapterId: String,
        mangaId: String,
        volume: String?,
        forceNetwork: Boolean
    ) = coroutineScope.launch {

    }

    private fun buildWorkRequest(chapterId: String, workId: UUID, mangaId: String, volume: String?): OneTimeWorkRequest {
        return OneTimeWorkRequestBuilder<ChapterDownloadWorker>()
            .setId(workId)
            .setInputData(
                Data.Builder()
                    .putBoolean(ChapterDownloadWorker.fetchImagesKey, true)
                    .putString(ChapterDownloadWorker.volumeNumberKey, volume ?: "0")
                    .putString(ChapterDownloadWorker.chapterIdKey, chapterId)
                    .putString(ChapterDownloadWorker.mangaIdKey, mangaId)
                    .build()
            )
            .build()
    }
}

sealed interface MangaReaderEvent

sealed class MangaReaderState {
    object Loading: MangaReaderState()
    data class Success(val pages: List<String> = emptyList()): MangaReaderState()
}

class MangaReaderScreen(
    val volume: String,
    val mangaId: String,
    val chapterId: String
): Screen {

    @Composable
    override fun Content() {
        val sm = getScreenModel<MangaReaderSM>()

        LaunchedEffect(Unit) {
            sm.loadMangaImages(chapterId, mangaId, volume, false)
        }
    }
}