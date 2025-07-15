package io.silv.reader2

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.request.ImageRequest
import io.silv.common.coroutine.suspendRunCatching
import io.silv.common.log.logcat
import io.silv.common.model.Page
import io.silv.data.util.ImageUtil
import io.silv.reader.loader.ReaderPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.BufferedSource


private fun process(
    viewer: PagerViewer,
    page: ReaderPage,
    imageSource: BufferedSource
): BufferedSource {
    if (page is InsertPage) {
        return splitInHalf(viewer, page, imageSource)
    }

    val isDoublePage = ImageUtil.isWideImage(imageSource)
    if (!isDoublePage) {
        return imageSource
    }

    onPageSplit(viewer, page)

    return splitInHalf(viewer, page, imageSource)
}

private fun splitInHalf(
    viewer: PagerViewer,
    page: ReaderPage,
    imageSource: BufferedSource
): BufferedSource {
    var side = when {
        viewer.l2r && page is InsertPage -> ImageUtil.Side.RIGHT
        !viewer.l2r && page is InsertPage -> ImageUtil.Side.LEFT
        viewer.l2r && page !is InsertPage -> ImageUtil.Side.LEFT
        else -> ImageUtil.Side.RIGHT
    }

    if (viewer.settings.dualPageInvert) {
        side = when (side) {
            ImageUtil.Side.RIGHT -> ImageUtil.Side.LEFT
            ImageUtil.Side.LEFT -> ImageUtil.Side.RIGHT
        }
    }

    return ImageUtil.splitInHalf(imageSource, side)
}

private fun onPageSplit(viewer: PagerViewer, page: ReaderPage) {
    val newPage = InsertPage(page)
    viewer.onPageSplit(page, newPage)
}


data class PageState(
    val image: ImageRequest?,
    val status: Page.State,
    val page: ReaderPage,
)

@Composable
fun pagePresenter(
    viewer: PagerViewer,
    context: Context,
    page: ReaderPage,
): PageState {

    var image by remember { mutableStateOf<ImageRequest?>(null) }
    val status by page.statusFlow.collectAsStateWithLifecycle()

    LaunchedEffect(viewer, page) {
        val loader = page.chapter.pageLoader ?: return@LaunchedEffect

        launch(Dispatchers.IO) {
            logcat { "launching load page ${page.url} ${page.imageUrl}" }
            loader.loadPage(page)
        }

        snapshotFlow { status }
            .filter { it == Page.State.READY }
            .collectLatest { state ->
                suspendRunCatching {
                    val source = withContext(Dispatchers.IO) {
                        page.stream!!().use { ins ->
                            process(viewer, page, Buffer().readFrom(ins))
                        }.readByteArray()
                    }
                    image = ImageRequest.Builder(context)
                        .data(source)
                        .crossfade(true)
                        .build()
                }
            }
    }

    return PageState(
        image,
        status,
        page,
    )
}
