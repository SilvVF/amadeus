package io.silv.explore.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import io.silv.model.SavableManga
import io.silv.ui.AnimatedBoxShimmer
import io.silv.ui.CenterBox
import io.silv.ui.MangaListItem
import io.silv.ui.theme.LocalSpacing
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.ceil
import kotlin.math.roundToInt

fun LazyListScope.recentMangaList(
    manga: LazyPagingItems<StateFlow<SavableManga>>,
    onTagClick: (manga: SavableManga, name: String) -> Unit,
    onBookmarkClick: (manga: SavableManga) -> Unit,
    onMangaClick: (manga: SavableManga) -> Unit,
) {
    items(
        count = ceil(manga.itemCount / 2f).roundToInt(),
        contentType = manga.itemContentType(),
        key = manga.itemKey()
    ) {
        val space = LocalSpacing.current
        val items = listOfNotNull(manga[it * 2], manga[(it * 2) + 1])
        Row {
            for(item in items) {
                val mangaItem by item.collectAsStateWithLifecycle()
                MangaListItem(
                    manga = mangaItem,
                    modifier = Modifier
                        .weight(1f)
                        .padding(space.large)
                        .clickable {
                            onMangaClick(mangaItem)
                        },
                    onTagClick = { name ->
                        onTagClick(mangaItem, name)
                    },
                    onBookmarkClick = {
                        onBookmarkClick(mangaItem)
                    }
                )
            }
        }
    }
    if (manga.loadState.refresh is LoadState.Loading) {
        items(4 , key = { it } ) {
            val space = LocalSpacing.current
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.padding(space.large)) {
                AnimatedBoxShimmer(Modifier.size(200.dp))
            }
        }
    } else if (manga.loadState.append is LoadState.Error || manga.loadState.refresh is LoadState.Error) {
        item {
            val space = LocalSpacing.current
            CenterBox(
                Modifier
                    .fillMaxSize()
                    .padding(space.large)
            ) {
                Button(onClick = { manga.retry() }) {
                    Text("Retry loading manga.")
                }
            }
        }
    }
}