package io.silv.amadeus.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.TopStart
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.domain.models.DomainManga
import io.silv.manga.network.mangadex.models.Status

@Composable
fun MainPoster(
    manga: DomainManga,
    modifier: Modifier
) {
    val ctx = LocalContext.current
    val space = LocalSpacing.current

    Box(modifier = Modifier
        .height(240.dp)
        .fillMaxWidth()) {
        BackgroundImageDarkened(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .align(TopStart),
            url = manga.coverArt
        )
        Row(
            Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(space.large)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(ctx)
                    .data(manga.coverArt)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.45f)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = space.med),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                MangaTitle(manga = manga)
                Spacer(Modifier.height(space.med))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                    Box(
                        modifier = Modifier.size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when(manga.status) {
                                    Status.completed -> Color.Cyan
                                    Status.ongoing -> Color.Green
                                    Status.cancelled -> Color.Red
                                    Status.hiatus -> Color.Yellow
                                }
                            )
                    )
                    Text(text = "Publication: ${manga.status.name}", modifier = Modifier.padding(horizontal = space.xs), fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun MangaTitle(modifier: Modifier= Modifier, manga: DomainManga,) {
    Column(modifier) {
        Text(
            text = manga.titleEnglish,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Start,
            fontSize = 22.sp,
            overflow = TextOverflow.Ellipsis,
            maxLines = 4
        )
        Text(
            text = manga.alternateTitles["ja"] ?: "",
            textAlign = TextAlign.Start,
        )
    }
}

@Composable
fun BackgroundImageDarkened(
    modifier: Modifier,
    url: String
) {
    val ctx = LocalContext.current
    Box(
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(ctx)
                    .data(url)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
        )
    }
}