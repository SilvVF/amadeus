package io.silv.manga.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.silv.manga.manga_view.MangaViewState
import io.silv.ui.composables.AnimatedBoxShimmer

fun LazyListScope.volumePosterItems(
    mangaState: MangaViewState
) {
    when (mangaState) {
        is MangaViewState.Loading -> item {
            VolumePostersPlaceHolder()
        }
        is MangaViewState.Success -> {
            items(mangaState.volumeToArt.toList().chunked(2)) {
                val context = LocalContext.current
                val space = io.silv.ui.theme.LocalSpacing.current
                Row(horizontalArrangement = Arrangement.Center) {
                    it.forEach { (_, url) ->
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(url.ifBlank { mangaState.manga.coverArt }).build(),
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)
                                .height(300.dp)
                                .padding(space.med),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VolumePostersPlaceHolder() {
    FlowRow {
        repeat(4) {
            AnimatedBoxShimmer(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(200.dp)
            )
        }
    }
}