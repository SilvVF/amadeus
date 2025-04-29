package io.silv.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import io.silv.data.manga.model.Manga
import io.silv.ui.CenterBox
import io.silv.ui.theme.LocalSpacing
import kotlinx.coroutines.flow.StateFlow

fun LazyGridScope.mangaGrid(
    manga: LazyPagingItems<StateFlow<Manga>>,
    cardType: CardType,
    onTagClick: (manga: Manga, name: String) -> Unit,
    onBookmarkClick: (manga: Manga) -> Unit,
    onMangaClick: (manga: Manga) -> Unit,
) {
    items(
        count = manga.itemCount,
        contentType = manga.itemContentType(),
        key = manga.itemKey(),
    ) { i ->

        val item = manga[i]?.collectAsStateWithLifecycle()
        val space = LocalSpacing.current

        item?.value?.let { manga ->
            MangaGridItem(
                manga = manga,
                cardType = cardType,
                modifier =
                Modifier
                    .padding(space.small)
                    .aspectRatio(2f / 3f)
                    .clickable {
                        onMangaClick(manga)
                    },
                onTagClick = { name ->
                    onTagClick(manga, name)
                },
                onBookmarkClick = {
                    onBookmarkClick(manga)
                },
            )
        }
    }

    if (manga.loadState.append is LoadState.Loading) {
        item(
            span = { GridItemSpan(maxLineSpan) },
            key = "load-state-append-manga",
        ) {
            CenterBox(Modifier.fillMaxWidth()) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        }
    }
    if (manga.loadState.append is LoadState.Error || manga.loadState.refresh is LoadState.Error) {
        item(
            key = "retry-loading",
            span = { GridItemSpan(maxLineSpan) },
        ) {
            CenterBox(
                Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = { manga.retry() },
                ) {
                    Text("Retry loading items")
                }
            }
        }
    }
}