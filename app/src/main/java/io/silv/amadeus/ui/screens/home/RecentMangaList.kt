package io.silv.amadeus.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import io.silv.amadeus.ui.composables.AnimatedBoxShimmer
import io.silv.amadeus.ui.composables.MangaListItem
import io.silv.amadeus.ui.shared.CenterBox
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.model.SavableManga
import kotlin.math.ceil
import kotlin.math.roundToInt

fun LazyListScope.recentMangaList(
    manga: LazyPagingItems<SavableManga>,
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
                MangaListItem(
                    manga = item,
                    modifier = Modifier
                        .weight(1f)
                        .padding(space.large)
                        .clickable {
                            onMangaClick(item)
                        },
                    onTagClick = { name ->
                        onTagClick(item, name)
                    },
                    onBookmarkClick = {
                        onBookmarkClick(item)
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
                    .padding(space.large)) {
                    Button(onClick = { manga.retry() }) {
                        Text("Retry loading manga.")
                    }
            }
        }
    }
}