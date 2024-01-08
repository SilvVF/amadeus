package io.silv.ui.composables

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import io.silv.domain.manga.model.Manga
import io.silv.ui.CenterBox
import io.silv.ui.theme.LocalSpacing
import kotlinx.coroutines.flow.StateFlow

fun LazyListScope.mangaList(
    manga: LazyPagingItems<StateFlow<Manga>>,
    onFavoriteClick: (manga: Manga) -> Unit,
    onMangaClick: (manga: Manga) -> Unit
) {
    if (manga.loadState.refresh is LoadState.Loading) {
        item("loading-refresh") {
            CenterBox(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        }
    } else {
        items(
            count = manga.itemCount,
            key = manga.itemKey(),
            contentType = manga.itemContentType(),
        ) { i ->

            val item = manga[i]?.collectAsStateWithLifecycle()
            val space = LocalSpacing.current

            item?.value?.let { manga ->
                MangaListItem(
                    manga = manga,
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(space.small),
                    onClick = onMangaClick,
                    onFavoriteClick = {
                        onFavoriteClick(manga)
                    },
                )
            }
        }
        if (manga.loadState.append == LoadState.Loading) {
            item(
                key = "append-loading",
            ) {
                CenterBox(Modifier.fillMaxWidth()) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }
        }
        if (manga.loadState.append is LoadState.Error || manga.loadState.refresh is LoadState.Error) {
            item(
                key = "retry-loading",
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
}