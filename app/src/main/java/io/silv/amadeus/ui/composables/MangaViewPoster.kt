package io.silv.amadeus.ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookOnline
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterEnd
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterStart
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.skydoves.orbital.Orbital
import com.skydoves.orbital.animateMovement
import com.skydoves.orbital.rememberContentWithOrbitalScope
import io.silv.manga.domain.models.DomainManga
import io.silv.amadeus.ui.shared.noRippleClickable
import io.silv.amadeus.ui.theme.LocalSpacing

@Composable
fun MangaViewPoster(
    modifier: Modifier,
    includeTopButtons: Boolean = true,
    manga: DomainManga,
    onReadNowClick: () -> Unit = {},
    onBookMarkClick: () -> Unit = {}
) {

    val ctx = LocalContext.current
    val space = LocalSpacing.current

    var isTransformed by rememberSaveable { mutableStateOf(false) }

    val movementSpec = SpringSpec<IntOffset>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = 200f
    )

    val poster = rememberContentWithOrbitalScope {
        Column {
            Text(
                text = manga.titleEnglish,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(CenterHorizontally)
                    .padding(bottom = space.med)
                    .animateMovement(
                        this@rememberContentWithOrbitalScope,
                        movementSpec
                    ),
                textAlign = TextAlign.Center,
                fontSize = if (isTransformed) {
                    32.sp
                } else {
                    18.sp
                },
                overflow = TextOverflow.Ellipsis,
                maxLines = 4
            )
            AsyncImage(
                model = ImageRequest.Builder(ctx)
                    .data(manga.coverArt)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = if (isTransformed) {
                    Modifier
                        .size(360.dp, 620.dp)
                } else {
                    Modifier
                        .size(130.dp, 220.dp)
                }
                    .animateMovement(this@rememberContentWithOrbitalScope, movementSpec),
            )
        }
    }

    Box(
        modifier = modifier
            .noRippleClickable { isTransformed = !isTransformed },
        contentAlignment = CenterEnd
    ) {
        Orbital(
            modifier = Modifier
                .fillMaxWidth(
                    if (isTransformed) 1f
                    else 0.4f
                )
                .align(CenterStart)
        ) {
            if (isTransformed) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    poster()
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    poster()
                }
            }
        }
        AnimatedVisibility(
            visible = !isTransformed,
            enter = slideInHorizontally(animationSpec = spring()) { it },
            exit = slideOutHorizontally(animationSpec = spring()) { it }
        ) {
            Column(
                Modifier
                    .fillMaxWidth(0.6f)
                    .align(CenterEnd)
                    .padding(space.med)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (includeTopButtons) {
                        Button(
                            onClick = onReadNowClick
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Visibility,
                                contentDescription = null,
                                Modifier.padding(end = space.small)
                            )
                            Text("Read Now")
                        }
                        IconButton(
                            onClick = onBookMarkClick
                        ) {
                            Icon(
                                imageVector =    if (manga.bookmarked)
                                    Icons.Filled.BookmarkRemove
                                else
                                    Icons.Filled.BookmarkBorder,
                                contentDescription = null
                            )
                        }
                    }
                }
                LazyRow {
                    items(manga.availableTranslatedLanguages) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(it) },
                            Modifier.padding(end = space.small),
                        )
                    }
                }
                LazyRow {
                    items(
                        emptyList<String>()// TODO()
                    ) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(it) },
                            Modifier.padding(end = space.small),
                        )
                    }
                }
                LazyColumn {
                    item {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    item {
                        Text(
                            text = manga.description
                        )
                    }
                }
            }
        }
    }
}