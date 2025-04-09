package io.silv.reader2

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.request.ImageRequest
import io.silv.common.log.logcat
import io.silv.common.model.Page
import io.silv.data.util.ImageUtil
import io.silv.reader.loader.ReaderPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import me.saket.telephoto.zoomable.ZoomableImageState
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import okio.Buffer
import okio.BufferedSource

@Stable
class PagerPageHolder(
    val viewer: PagerViewer,
    val page: ReaderPage,
    val scope: CoroutineScope
) {
    val status = page.statusFlow.stateIn(
        scope,
        SharingStarted.Eagerly,
        initialValue = Page.State.QUEUE
    )

    var image by mutableStateOf<ImageRequest?>(null)
    var loadJob: Job? = null

    fun load() {
        loadJob?.cancel()

        loadJob = scope.launch {
            val loader = page.chapter.pageLoader ?:return@launch
            logcat { "pageLoader ${page.chapter.pageLoader}" }
            logcat { "streamFn ${page.stream}" }
            supervisorScope {

                launch(Dispatchers.IO) {
                    logcat { "launching load page ${page.url} ${page.imageUrl}" }
                    loader.loadPage(page)
                }

                page.statusFlow.collectLatest {
                    if (it != Page.State.READY || image != null) return@collectLatest

                    val streamFn = page.stream ?: return@collectLatest

                    val (source, isAnimated, background) = withContext(Dispatchers.IO) {
                        val source = streamFn().use { ins ->
                            process(page, Buffer().readFrom(ins))
                        }
                        val isAnimated = ImageUtil.isAnimatedAndSupported(source)
                        val background = if (!isAnimated && viewer.config.automaticBackground) {
                            ImageUtil.chooseBackground(viewer.context, source.peek().inputStream())
                        } else {
                            null
                        }
                        Triple(source.readByteArray(), isAnimated, background)
                    }
                    image = ImageRequest.Builder(viewer.context)
                        .data(source)
                        .crossfade(true)
                        .build()
                }
            }
        }
    }

    fun dispose() {
        loadJob?.cancel()
    }

    // needed bc zoomable consumes clicks but
    // passes the offset to onClick
    fun dispatchClick(offset: Offset, size: IntSize) {
        viewer.handleClickEvent(offset, size)
    }

    private fun process(page: ReaderPage, imageSource: BufferedSource): BufferedSource {
        if (viewer.config.dualPageRotateToFit) {
            return rotateDualPage(imageSource)
        }

        if (!viewer.config.dualPageSplit) {
            return imageSource
        }

        if (page is InsertPage) {
            return splitInHalf(imageSource)
        }

        val isDoublePage = ImageUtil.isWideImage(imageSource)
        if (!isDoublePage) {
            return imageSource
        }

        onPageSplit(page)

        return splitInHalf(imageSource)
    }

    private fun rotateDualPage(imageSource: BufferedSource): BufferedSource {
        val isDoublePage = ImageUtil.isWideImage(imageSource)
        return if (isDoublePage) {
            val rotation = if (viewer.config.dualPageRotateToFitInvert) -90f else 90f
            ImageUtil.rotateImage(imageSource, rotation)
        } else {
            imageSource
        }
    }

    private fun splitInHalf(imageSource: BufferedSource): BufferedSource {
        var side = when {
            viewer.l2r && page is InsertPage -> ImageUtil.Side.RIGHT
            !viewer.l2r && page is InsertPage -> ImageUtil.Side.LEFT
            viewer.l2r && page !is InsertPage -> ImageUtil.Side.LEFT
            !viewer.l2r && page !is InsertPage -> ImageUtil.Side.RIGHT
            else -> error("We should choose a side!")
        }

        if (viewer.config.dualPageInvert) {
            side = when (side) {
                ImageUtil.Side.RIGHT -> ImageUtil.Side.LEFT
                ImageUtil.Side.LEFT -> ImageUtil.Side.RIGHT
            }
        }

        return ImageUtil.splitInHalf(imageSource, side)
    }

    private fun onPageSplit(page: ReaderPage) {
        val newPage = InsertPage(page)
        viewer.onPageSplit(page, newPage)
    }
}

@Composable
fun PagerPage(
    pagerPageHolder: PagerPageHolder,
    modifier: Modifier = Modifier,
    zoomState: ZoomableImageState = rememberZoomableImageState(),
) {
    val status by pagerPageHolder.status.collectAsStateWithLifecycle()

    DisposableEffect(pagerPageHolder) {
        logcat { "calling load job" }
        pagerPageHolder.load()
        onDispose {
            pagerPageHolder.dispose()
        }
    }

    Box(modifier) {
        when (status) {
            Page.State.QUEUE, Page.State.LOAD_PAGE, Page.State.DOWNLOAD_IMAGE -> {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.align(
                        Alignment.Center
                    )
                )
            }

            Page.State.READY -> {

                if (pagerPageHolder.image == null) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.align(
                            Alignment.Center
                        )
                    )
                    return@Box
                }
                var size by remember { mutableStateOf(IntSize.Zero) }
                ZoomableAsyncImage(
                    state = zoomState,
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { size = it },
                    model = pagerPageHolder.image,
                    contentDescription = pagerPageHolder.page.url,
                    onClick = {
                        if (size != IntSize.Zero) {
                            pagerPageHolder.dispatchClick(it, size)
                        }
                    }
                )
            }

            Page.State.ERROR -> Text("error", Modifier.align(Alignment.Center))
        }
    }
}
