package io.silv.amadeus.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.silv.amadeus.ui.shared.fillMaxAfterMesaure
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.domain.models.SavableManga
import io.silv.manga.network.mangadex.models.Status

@Composable
fun MainPoster(
    manga: SavableManga,
    modifier: Modifier
) {
    val ctx = LocalContext.current
    val space = LocalSpacing.current

    Box(modifier = modifier) {
        BackgroundImageDarkened(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxAfterMesaure(this, 0.8f)
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
            MangaInfo(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = space.med),
                manga = manga
            )
        }
    }
}

@Composable
fun MangaInfo(
    modifier: Modifier = Modifier,
    manga: SavableManga
) {
    val space = LocalSpacing.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        MangaTitle(
            modifier = Modifier
                .padding(bottom = space.med)
                .fillMaxWidth()
                .wrapContentHeight(),
            title = manga.titleEnglish,
            altTitle = manga.alternateTitles["ja-ro"]?.ifEmpty { null } ?: manga.titleEnglish,
            authors = (manga.authors + manga.artists).joinToString()
        )
        PublicationStatusIndicator(status = manga.status, year = manga.year)
    }
}

@Composable
fun PublicationStatusIndicator(
    status: Status,
    year: Int?
) {
    val space = LocalSpacing.current
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when (status) {
                            Status.completed -> Color.Cyan
                            Status.ongoing -> Color.Green
                            Status.cancelled -> Color.Red
                            Status.hiatus -> Color.Yellow
                        }
                    )
            )
            year?.let {
                Text(
                    text = "Publication: $year",
                    modifier = Modifier.padding(horizontal = space.xs),
                    fontSize = 16.sp
                )
            }
        }
        Text(
            modifier = Modifier.padding(start = 32.dp),
            text = "Status, ${status.name}",
            fontSize = 16.sp,
        )
    }
}

@Composable
fun MangaTitle(
    modifier: Modifier = Modifier,
    title: String,
    altTitle: String,
    authors: String
) {
    val space = LocalSpacing.current
    Column(modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Start,
            overflow = TextOverflow.Ellipsis,
            maxLines = 4
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = space.xs),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = altTitle,
                textAlign = TextAlign.Start,
                fontSize = 16.sp,
            )
            Text(
                text = authors,
                textAlign = TextAlign.Start,
                fontSize = 12.sp,
            )
        }
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