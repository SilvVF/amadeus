package io.silv.manga.composables

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.silv.common.model.MangaCover
import io.silv.manga.manga_view.MangaViewState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

fun LazyListScope.volumePosterItems(
    state: MangaViewState.Success
) {
    items(
        items = state.volumeToArtList,
        key = { item -> item.toString() }
    ) { (_, url) ->
        val context = LocalContext.current
        val space = io.silv.ui.theme.LocalSpacing.current
        val cover = remember(state.manga, url) {
            MangaCover(
                mangaId = state.manga.id,
                url = url,
                isMangaFavorite = state.manga.bookmarked,
                lastModified = state.manga.updatedAt.toInstant(TimeZone.currentSystemDefault()).epochSeconds
            )
        }
        AsyncImage(
            model = cover,
            contentDescription = null,
            modifier = Modifier
                .height(300.dp)
                .padding(space.med),
            contentScale = ContentScale.Fit
        )
    }
}