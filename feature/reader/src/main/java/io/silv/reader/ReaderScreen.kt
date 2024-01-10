@file:OptIn(ExperimentalFoundationApi::class)

package io.silv.reader

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.silv.common.model.Page
import io.silv.datastore.ReaderPrefs
import io.silv.datastore.collectAsState
import io.silv.domain.chapter.model.Chapter
import io.silv.domain.manga.model.Manga
import io.silv.reader.composables.GestureHintOverlay
import io.silv.reader.composables.ReaderMenuOverlay
import io.silv.reader.loader.ReaderChapter
import io.silv.ui.CenterBox
import io.silv.ui.Converters
import io.silv.ui.openOnWeb
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import java.nio.ByteBuffer
import kotlin.math.roundToInt

class ReaderScreen(
    val mangaId: String,
    val chapterId: String,
) : Screen {

    @Stable
    private var savedStateChapterId = chapterId

    @Composable
    override fun Content() {

        val screenModel = getScreenModel<ReaderScreenModel> { parametersOf(mangaId, savedStateChapterId) }

        val state = screenModel.state.collectAsStateWithLifecycle().value
        val lifecycle = LocalLifecycleOwner.current

        DisposableEffect(lifecycle.lifecycle) {

            val observer = LifecycleEventObserver { source, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> screenModel.restartReadTimer()
                    Lifecycle.Event.ON_PAUSE -> screenModel.flushReadTimer()
                    else -> Unit
                }
            }

            lifecycle.lifecycle.addObserver(
                observer = observer
            )
            onDispose { lifecycle.lifecycle.removeObserver(observer) }
        }

        DisposableEffect(Unit) {
            onDispose { screenModel.flushReadTimer() }
        }


        LaunchedEffect(key1 = Unit) {
            screenModel.state.map { it.viewerChapters?.currChapter }
                .filterNotNull()
                .distinctUntilChanged()
                .collect {
                    savedStateChapterId = it.chapter.id
                }
        }

        Box(Modifier.fillMaxSize()) {
            if (state.viewerChapters != null && state.manga != null) {
                HorizontalReader(
                    viewerChapters = state.viewerChapters,
                    onPageChange = screenModel::pageChanged,
                    chapterList = state.chapters,
                    loadPrevChapter = screenModel::loadPreviousChapter,
                    loadNextChapter = screenModel::loadNextChapter,
                    manga = state.manga
                )
            } else {
                CenterBox(Modifier.fillMaxSize()) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HorizontalReader(
    viewerChapters: ViewerChapters,
    chapterList: ImmutableList<Chapter>,
    manga: Manga,
    onPageChange: (readerChapter: ReaderChapter, page: Page) -> Unit,
    loadNextChapter: () -> Unit,
    loadPrevChapter: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.currentOrThrow

    val readerChapter = viewerChapters.currChapter

    val pagerState =
        rememberPagerState(
            initialPage = readerChapter.requestedPage,
        ) {
            readerChapter.pages?.size ?: 0
        }

    LaunchedEffect(readerChapter.chapter) {
        pagerState.scrollToPage(readerChapter.requestedPage)
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect {
            readerChapter.requestedPage = it
            onPageChange(readerChapter, readerChapter.pages?.getOrNull(it) ?: return@collect)
        }
    }

    val layoutDirection by ReaderPrefs.layoutDirection.collectAsState(
        defaultValue = LayoutDirection.Rtl,
        converter = Converters.LayoutDirectionConverter
    )

    fun scrollToNextPage() {
        scope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage + 1)
        }
    }

    fun scrollToPage(page: Int) {
        scope.launch {
            pagerState.animateScrollToPage(page)
        }
    }

    fun scrollToPrevPage() {
        scope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage - 1)
        }
    }

    var menuVisible by remember { mutableStateOf(false) }

    ReaderMenuOverlay(
        readerChapter =  { readerChapter },
        chapters = { chapterList },
        manga = { manga },
        menuVisible = { menuVisible },
        currentPage = { pagerState.currentPage },
        onDismissRequested = { menuVisible = false },
        changePage = ::scrollToPage,
        loadNextChapter = loadNextChapter,
        loadPrevChapter = loadPrevChapter,
        layoutDirection = layoutDirection,
        onBackArrowClick = {
            navigator.pop()
        },
        onViewOnWebClick = {
              context.openOnWeb(
                  "https://mangadex.org/chapter/${readerChapter.chapter.id}/${readerChapter.requestedPage}",
                  "view chapter on mangadex."
              )
                  .onFailure {
                      Toast.makeText(context, "couldn't find a way to open chapter", Toast.LENGTH_SHORT).show()
                  }
        }
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides layoutDirection
        ) {
            GestureHintOverlay {
                HorizontalPager(
                    state = pagerState,
                    beyondBoundsPageCount = 1,
                    modifier = Modifier
                        .fillMaxSize()
                        .zoneClickable {
                            when (layoutDirection) {
                                LayoutDirection.Ltr -> when (it) {
                                    Zone.LEFT -> scrollToPrevPage()
                                    Zone.MIDDLE -> menuVisible = !menuVisible
                                    Zone.RIGHT -> scrollToNextPage()
                                }

                                LayoutDirection.Rtl -> when (it) {
                                    Zone.LEFT -> scrollToNextPage()
                                    Zone.MIDDLE -> menuVisible = !menuVisible
                                    Zone.RIGHT -> scrollToPrevPage()
                                }
                            }
                        }
                ) { i ->

                    val page = readerChapter.pages?.getOrNull(i)
                        ?: return@HorizontalPager

                    val status by page.statusFlow.collectAsState()

                    LaunchedEffect(page) {
                        readerChapter.pageLoader?.loadPage(page)
                    }

                    when (status) {
                        Page.State.READY -> {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(
                                        page.stream?.let {
                                            ByteBuffer.wrap(it().readBytes())
                                        }
                                            ?: page.imageUrl
                                    )
                                    .build(),
                                modifier = Modifier.fillMaxSize(),
                                contentDescription = null
                            )
                        }
                        Page.State.QUEUE -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "Queued")
                                CircularProgressIndicator()
                            }
                        }
                        Page.State.LOAD_PAGE, Page.State.DOWNLOAD_IMAGE -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                CircularProgressIndicator(
                                    progress = page.progress.toFloat(),
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                        Page.State.ERROR -> {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "error"
                                )
                                Text("A problem occurred while loading the page")
                            }
                        }
                    }
                }
            }
        }
    }
}


private enum class Zone {
    LEFT, MIDDLE, RIGHT
}

private fun Modifier.zoneClickable(
    onClick: (Zone) -> Unit
) = pointerInput(Unit) {

    val zoneSplit = this.size.width / 3

    detectTapGestures {
        val zone = when(it.x.roundToInt()){
            in 0..zoneSplit -> Zone.LEFT
            in zoneSplit..(size.width - zoneSplit) -> Zone.MIDDLE
            else -> Zone.RIGHT
        }
        onClick(zone)
    }
}
