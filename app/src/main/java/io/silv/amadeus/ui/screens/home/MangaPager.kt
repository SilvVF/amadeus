package io.silv.amadeus.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.silv.amadeus.ui.composables.BlurImageBackground
import io.silv.amadeus.ui.composables.MangaGenreTags
import io.silv.amadeus.ui.composables.TranslatedLanguageTags
import io.silv.amadeus.ui.shared.CenterBox
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.amadeus.types.SavableManga
import kotlinx.coroutines.launch


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MangaPager(
    mangaList: List<SavableManga>,
    onMangaClick: (manga: SavableManga) -> Unit,
    onBookmarkClick: (manga: SavableManga) -> Unit,
    onTagClick: (name: String, id: String) -> Unit,
) {
    val space = LocalSpacing.current
    val context = LocalContext.current
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()

    HorizontalPager(
        pageCount = mangaList.size,
        state = pagerState,
        modifier = Modifier
            .height(240.dp)
            .fillMaxWidth()
    ) { page ->

        val manga = mangaList[page]

        BlurImageBackground(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    onMangaClick(manga)
                },
            url = manga.coverArt
        ) {

            val lazyListState = rememberLazyListState()

            LazyColumn(
                state = lazyListState
            ) {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .padding(space.med),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        CenterBox(Modifier.height(230.dp)) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(manga.coverArt)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Inside,
                                modifier = Modifier
                                    .fillMaxWidth(0.4f)
                            )
                        }
                        Spacer(modifier = Modifier.width(space.med))
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.Start) {
                            Text(
                                text = manga.titleEnglish,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 2,
                                fontSize = 20.sp,
                                overflow = TextOverflow.Ellipsis
                            )
                            TranslatedLanguageTags(
                                tags = manga.availableTranslatedLanguages,
                                modifier = Modifier.fillMaxWidth()
                            )
                            MangaGenreTags(
                                tags = manga.tagToId.keys.toList(),
                                modifier = Modifier.fillMaxWidth(),
                                onTagClick = { name ->
                                    manga.tagToId[name]?.let {
                                        onTagClick(name, it)
                                    }
                                }
                            )
                            Column(
                                Modifier.weight(1f),
                                verticalArrangement = Arrangement.Bottom,
                                horizontalAlignment = Alignment.End
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    IconButton(onClick = { onBookmarkClick(manga) }) {
                                        Icon(
                                            imageVector = if (manga.bookmarked)
                                                Icons.Filled.Favorite
                                            else
                                                Icons.Outlined.FavoriteBorder,
                                            contentDescription = null
                                        )
                                    }
                                    Text(
                                        if (manga.bookmarked)
                                            "In library"
                                        else
                                            "Add to library"
                                    )
                                }
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "NO.${page + 1}",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                pagerState.animateScrollToPage(page - 1)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.KeyboardArrowLeft,
                                            contentDescription = null
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                pagerState.animateScrollToPage(page + 1)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.KeyboardArrowRight,
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
