package io.silv.reader2

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.request.ImageRequest
import io.silv.common.coroutine.suspendRunCatching
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import me.saket.telephoto.zoomable.ZoomableImageState
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import okio.Buffer
import okio.BufferedSource


@Composable
fun PagerPage(
    pageState: PageState,
    modifier: Modifier = Modifier,
    zoomState: ZoomableImageState = rememberZoomableImageState(),
) {
    Box(modifier) {
        when {
            pageState.status == Page.State.ERROR -> Text("error", Modifier.align(Alignment.Center))
            pageState.status in listOf(
                Page.State.QUEUE,
                Page.State.LOAD_PAGE,
                Page.State.DOWNLOAD_IMAGE
            ) || pageState.image == null -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(
                        Alignment.Center
                    )
                )
            }
            else -> {
                var size by remember { mutableStateOf(IntSize.Zero) }
                ZoomableAsyncImage(
                    state = zoomState,
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { size = it },
                    model = pageState.image,
                    contentDescription = pageState.page.url,
                    onClick = {
                        if (size != IntSize.Zero) {
                            pageState.events(ViewerEvent.Click(it, size))
                        }
                    }
                )
            }
        }
    }
}
