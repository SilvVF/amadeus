package io.silv.amadeus.ui.screens.manga_reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
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
    state: PagerState
) {
    val ctx = LocalContext.current

    HorizontalPager(
        contentPadding = PaddingValues(horizontal = 100.dp),
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f),
        pageCount = imageUris.size,
        pageSize = PageSize.Fill,
        state = state
    ){ page ->
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