package io.silv.explore.composables

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import cafe.adriel.voyager.navigator.LocalNavigator
import io.silv.model.SavableManga
import io.silv.ui.composables.AnimatedBoxShimmer
import io.silv.ui.composables.MangaListItemSideTitle

@Composable
fun TrendingMangaList(
    manga: LazyPagingItems<SavableManga>,
    onBookmarkClick: (manga: SavableManga) -> Unit
) {
    val space = io.silv.ui.theme.LocalSpacing.current
    val navigator = LocalNavigator.current
    val context = LocalContext.current

    LaunchedEffect(key1 = manga.loadState) {
        if(manga.loadState.refresh is LoadState.Error) {
            Toast.makeText(
                context,
                "Check network connection",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LazyRow(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(
            count = manga.itemCount,
            key = manga.itemKey(),
            contentType = manga.itemContentType()
        ) { i ->
            manga[i]?.let {
                MangaListItemSideTitle(
                    manga = it,
                    modifier = Modifier
                        .padding(space.med)
                        .width(200.dp),
                    onTagClick = { tag ->
                        it.tagToId[tag]?.let { id ->
//                            navigator?.push(
//                                MangaFilterScreen(tag, id)
//                            )
                        }
                    },
                    onBookmarkClick = {
                        onBookmarkClick(it)
                    },
                    index = i,
                    onMangaImageClick = {
//                        navigator?.push(
//                            MangaViewScreen(it)
//                        )
                    }
                )
            }
        }
        if (manga.loadState.refresh is LoadState.Loading) {
            items(4) {
                io.silv.ui.CenterBox(Modifier.size(200.dp)) {
                    AnimatedBoxShimmer(
                        Modifier
                            .size(200.dp)
                            .padding(space.med)
                    )
                }
            }
        }
        item {
            if (manga.loadState.append is LoadState.Loading) {
                io.silv.ui.CenterBox(Modifier.size(200.dp)) {
                    CircularProgressIndicator()
                }
            }
            if (manga.loadState.append is LoadState.Error || manga.loadState.refresh is LoadState.Error) {
                io.silv.ui.CenterBox(Modifier.size(200.dp)) {
                    Button(onClick = { manga.retry() }) {
                        Text("Retry loading manga")
                    }
                }
            }
        }
    }
}