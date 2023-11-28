package io.silv.amadeus.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import io.silv.amadeus.ui.composables.AnimatedBoxShimmer
import io.silv.amadeus.ui.composables.MangaListItemSideTitle
import io.silv.amadeus.ui.screens.manga_filter.MangaFilterScreen
import io.silv.amadeus.ui.screens.manga_view.MangaViewScreen
import io.silv.amadeus.ui.shared.CenterBox
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.model.SavableManga

@Composable
fun TrendingMangaList(
    manga: LazyPagingItems<SavableManga>,
    onBookmarkClick: (manga: SavableManga) -> Unit
) {
    val space = LocalSpacing.current
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
    if (manga.itemCount <= 0 && manga.loadState.refresh != LoadState.Loading) {
        CenterBox(Modifier.fillMaxWidth().padding(space.med)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "unable to load, make sure to check your network connection.")
                Button(onClick = { manga.retry() }) {
                    Text("Retry loading manga")
                }
            }
        }
    } else {
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
                                navigator?.push(
                                    MangaFilterScreen(tag, id)
                                )
                            }
                        },
                        onBookmarkClick = {
                            onBookmarkClick(it)
                        },
                        index = i,
                        onMangaImageClick = {
                            navigator?.push(
                                MangaViewScreen(it)
                            )
                        }
                    )
                }
            }
            if (manga.loadState.refresh is LoadState.Loading) {
                items(4) {
                    CenterBox(Modifier.size(200.dp)) {
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
                    CenterBox(Modifier.size(200.dp)) {
                        CircularProgressIndicator()
                    }
                }
                if (manga.loadState.append is LoadState.Error || manga.loadState.refresh is LoadState.Error) {
                    CenterBox(Modifier.size(200.dp)) {
                        Button(onClick = { manga.retry() }) {
                            Text("Retry loading manga")
                        }
                    }
                }
            }
        }
    }
}