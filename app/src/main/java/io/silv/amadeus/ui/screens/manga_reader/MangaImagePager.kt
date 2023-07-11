package io.silv.amadeus.ui.screens.manga_reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MangaImagePager(
    imageUris: List<String>,
    horizontalState: PagerState,
    modifier: Modifier,
) {
    val ctx = LocalContext.current
    HorizontalPager(
        contentPadding = PaddingValues(horizontal = 0.dp),
        pageCount = imageUris.size,
        modifier = modifier,
        pageSize = PageSize.Fill,
        state = horizontalState
    ) { page ->
        AsyncImage(
            model = ImageRequest.Builder(ctx)
                .data(imageUris[page])
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}