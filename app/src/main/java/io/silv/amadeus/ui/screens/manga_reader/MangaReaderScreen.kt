package io.silv.amadeus.ui.screens.manga_reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import cafe.adriel.voyager.core.model.coroutineScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.silv.amadeus.ui.composables.AnimatedBoxShimmer
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.local.entity.relations.MangaWithChapters
import io.silv.manga.local.workers.ChapterDownloadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

class MangaReaderSM(
    private val workManager: WorkManager,
    private val savedMangaRepository: SavedMangaRepository,
    private val mangaId: String,
    initialChapterId: String,
): AmadeusScreenModel<MangaReaderEvent>() {

    private val chapterId = MutableStateFlow(initialChapterId)

    private var saveMangaJob: Job? = null

    val mangaWithChapters = combineMangaWithChapter(
        chapterId,
        savedMangaRepository.getSavedMangaWithChapter(mangaId),
        mangaNotFound = {
            if (saveMangaJob == null) {
                saveMangaJob = CoroutineScope(Dispatchers.IO).launch {
                    savedMangaRepository.saveManga(mangaId)
                }
            }
        },
        chapterNotFound = {},
        notEnoughImages = {
            loadMangaImages()
        }
    )
        .stateInUi(MangaReaderState.Loading)

    private fun loadMangaImages() = coroutineScope.launch {
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

private fun combineMangaWithChapter(
    chapterId: Flow<String>,
    manga: Flow<MangaWithChapters?>,
    mangaNotFound: suspend () -> Unit,
    chapterNotFound: suspend () -> Unit,
    notEnoughImages: suspend (prev: Int) -> Unit
) = combine(
    chapterId,
    manga
) { cid, mangaWithChapters ->
    if (mangaWithChapters == null) {
       mangaNotFound()
    }
    val chapter = mangaWithChapters?.chapters?.find { it.id == cid }
    if(chapter == null) {
        chapterNotFound()
    }
    val images = chapter?.chapterImages?.takeIf { it.isNotEmpty() }
    if (images != null) {
        if (chapter.pages < images.size) {
            notEnoughImages(images.size)
        }
        MangaReaderState.Success(images)
    } else {
        notEnoughImages(0)
        MangaReaderState.Loading
    }
}

sealed interface MangaReaderEvent

sealed class MangaReaderState {
    object Loading: MangaReaderState()
    data class Success(val pages: List<String> = emptyList()): MangaReaderState()
}

class MangaReaderScreen(
    private val mangaId: String,
    private val chapterId: String
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
            AnimatedBoxShimmer(modifier = Modifier.padding(100.dp).fillMaxSize())
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