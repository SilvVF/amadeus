package io.silv.amadeus.ui.screens.manga_reader

import android.widget.ImageView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import cafe.adriel.voyager.core.model.coroutineScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.domain.repositorys.ChapterInfoRepository
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.local.workers.ChapterDownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.guava.asDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.parameter.parametersOf
import java.util.UUID

class MangaReaderSM(
    private val workManager: WorkManager,
    private val savedMangaRepository: SavedMangaRepository,
    private val mangaId: String,
    private val initialChapterId: String,
): AmadeusScreenModel<MangaReaderEvent>() {

    private val chapterId = MutableStateFlow(initialChapterId)

    val mangaWithChapters = combine(
        chapterId,
        savedMangaRepository.getSavedMangaWithChapter(mangaId)
    ) { chapterId, mangaWithChapters ->
        val chapterImages = mangaWithChapters?.chapters
            ?.find { it.id == chapterId }
            ?.chapterImages
            ?.takeIf { it.isNotEmpty() }
        if (chapterImages != null) {
            MangaReaderState.Success(chapterImages)
        } else {
            loadMangaImages()
            MangaReaderState.Loading
        }
    }
        .stateInUi(MangaReaderState.Loading)

    private fun loadMangaImages() = coroutineScope.launch {
        withContext(Dispatchers.IO) {
            workManager.enqueueUniqueWork(
                chapterId.value,
                ExistingWorkPolicy.KEEP,
                ChapterDownloadWorker.downloadWorkRequest(
                    listOf(chapterId.value),
                    mangaId
                )
            )
        }
    }

}

sealed interface MangaReaderEvent

sealed class MangaReaderState {
    object Loading: MangaReaderState()
    data class Success(val pages: List<String> = emptyList()): MangaReaderState()
}

class MangaReaderScreen(
    val mangaId: String,
    val chapterId: String
): Screen {

    @Composable
    override fun Content() {
        val sm = getScreenModel<MangaReaderSM>() { parametersOf(mangaId, chapterId) }

        val state by sm.mangaWithChapters.collectAsStateWithLifecycle()

        MangaReader(state = state)
    }
}

@Composable
fun MangaReader(
    state: MangaReaderState
) {
    when (state) {
        MangaReaderState.Loading -> {

        }
        is MangaReaderState.Success -> {
            MangaImagePager(imageUris = state.pages)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MangaImagePager(
    imageUris: List<String>
) {
    val ctx = LocalContext.current

    LaunchedEffect(key1 = imageUris) {
        println("[Images - ${imageUris.size}]$imageUris")
    }

    HorizontalPager(
        modifier = Modifier.fillMaxSize(),
        pageCount = imageUris.size,
        pageSize = PageSize.Fixed(300.dp)
    ){ page ->
        AsyncImage(
            model = ImageRequest.Builder(ctx)
                .data(imageUris[page])
                .build(),
            contentDescription = null
        )
    }
}