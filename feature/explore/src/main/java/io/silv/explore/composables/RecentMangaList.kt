package io.silv.explore.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import io.silv.model.SavableManga
import io.silv.ui.AnimatedBoxShimmer
import io.silv.ui.CenterBox
import io.silv.ui.MangaListItem
import io.silv.ui.theme.LocalSpacing

fun LazyGridScope.recentMangaList(
    manga: LazyPagingItems<SavableManga>,
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
                modifier = Modifier
                    .padding(space.med)
                    .height((LocalConfiguration.current.screenHeightDp / 2.6f).dp)
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
    if (manga.loadState.refresh is LoadState.Loading) {
        items(
            count = 4,
            key = { it } 
        ) {
            val space = LocalSpacing.current
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.padding(space.large)
            ) {
                AnimatedBoxShimmer(
                    Modifier
                        .weight(1f)
                        .height(200.dp))
                Spacer(modifier = Modifier.width(12.dp))
                AnimatedBoxShimmer(
                    Modifier
                        .weight(1f)
                        .height(200.dp))
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