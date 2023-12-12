package io.silv.explore.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Modifier
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import io.silv.model.SavableManga
import io.silv.ui.CenterBox
import io.silv.ui.composables.CardType
import io.silv.ui.composables.MangaListItem
import io.silv.ui.theme.LocalSpacing

fun LazyGridScope.mangaGrid(
    manga: LazyPagingItems<SavableManga>,
    cardType: CardType,
    onTagClick: (manga: SavableManga, name: String) -> Unit,
    onBookmarkClick: (manga: SavableManga) -> Unit,
    onMangaClick: (manga: SavableManga) -> Unit,
) {
    items(
        count = manga.itemCount,
        contentType = manga.itemContentType(),
        key = manga.itemKey()
    ) {i ->

        val item = manga[i]
        val space = LocalSpacing.current

        item?.let { manga ->
            MangaListItem(
                manga = item,
                cardType = cardType,
                modifier = Modifier
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
                }
            )
        }
    }
    if (manga.loadState.append is LoadState.Loading) {
        item(
            key = "load-state-append-manga"
        ) {
            CenterBox(Modifier.fillMaxWidth()) {
                CircularProgressIndicator()
            }
        }
    }
}