package io.silv.explore.composables

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import cafe.adriel.voyager.navigator.LocalNavigator
import io.silv.model.SavableManga
import io.silv.navigation.SharedScreen
import io.silv.navigation.push
import io.silv.ui.AnimatedBoxShimmer
import io.silv.ui.CenterBox
import io.silv.ui.MangaListItem
import io.silv.ui.PullRefresh
import io.silv.ui.header

@Composable
fun SearchItemsPagingList(
    modifier: Modifier = Modifier,
    items: LazyPagingItems<SavableManga>,
    onMangaClick: (manga: SavableManga) -> Unit,
    onBookmarkClick: (manga: SavableManga) -> Unit
) {
    val space = io.silv.ui.theme.LocalSpacing.current
    val navigator = LocalNavigator.current
    val context = LocalContext.current

    LaunchedEffect(key1 = items.loadState) {
        if(items.loadState.refresh is LoadState.Error) {
            Toast.makeText(
                context,
                "Error: " + (items.loadState.refresh as LoadState.Error).error.message,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    PullRefresh(
        refreshing = items.loadState.refresh is LoadState.Loading,
        onRefresh = { items.refresh() }
    ) {
        LazyVerticalGrid(
            modifier = modifier,
            columns = GridCells.Fixed(2)
        ) {
            items(
                count = items.itemCount,
                key = items.itemKey(),
                contentType = items.itemContentType()
            ) { i ->
                items[i]?.let {
                    MangaListItem(
                        manga = it,
                        modifier = Modifier
                            .padding(space.large)
                            .height((LocalConfiguration.current.screenHeightDp / 2.6f).dp)
                            .clickable {
                                onMangaClick(it)
                            },
                        onBookmarkClick = {
                            onBookmarkClick(it)
                        },
                        onTagClick = { name ->
                            it.tagToId[name]?.let { id ->
                                navigator?.push(
                                    SharedScreen.MangaFilter(name, id)
                                )
                            }
                        }
                    )
                }
            }
            if (items.loadState.refresh is LoadState.Loading) {
                items(8, key = { it }) {
                    AnimatedBoxShimmer(Modifier.padding(space.large).fillMaxWidth().height((LocalConfiguration.current.screenHeightDp / 2.6f).dp))
                }
            }
            header(
                key = "loading-indicator"
            ) {
                if (items.loadState.append is LoadState.Loading) {
                    CenterBox(Modifier.size(200.dp)) {
                        CircularProgressIndicator()
                    }
                }
                if (items.loadState.append is LoadState.Error || items.loadState.refresh is LoadState.Error) {
                    CenterBox(Modifier.size(200.dp)) {
                        Button(onClick = { items.retry() }) {
                            Text("Retry loading manga")
                        }
                    }
                }
            }
        }
    }
}