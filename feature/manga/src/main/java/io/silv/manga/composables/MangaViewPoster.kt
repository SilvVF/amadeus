package io.silv.manga.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.TopStart
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import io.silv.manga.manga_view.StatsUiState
import io.silv.ui.fillMaxAfterMesaure
import io.silv.common.filterUnique
import io.silv.common.model.Status
import io.silv.model.SavableManga

@Composable
fun MainPoster(
    manga: SavableManga,
    modifier: Modifier,
    padding: PaddingValues,
    viewMangaArtClick: () -> Unit,
    statsState: StatsUiState,
) {
    val ctx = LocalContext.current
    val space = io.silv.ui.theme.LocalSpacing.current

    Box(modifier = modifier) {
        BackgroundImageDarkened(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxAfterMesaure(this, 1f)
                .align(TopStart),
            url = manga.coverArt
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(space.med)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(ctx)
                    .data(manga.coverArt)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.35f)
                    .clip(RoundedCornerShape(12.dp))
            )
            MangaInfo(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = space.med),
                manga = manga,
            )
        }
    }
}

@Composable
fun MangaInfo(
    modifier: Modifier = Modifier,
    manga: SavableManga,
) {
    val space = io.silv.ui.theme.LocalSpacing.current
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
            authors = remember(manga) { (manga.authors + manga.artists).filterUnique { it }.joinToString() }
        )
        PublicationStatusIndicator(status = manga.status, year = manga.year)
    }
}

@Composable
fun PublicationStatusIndicator(
    status: Status,
    year: Int?
) {
    val space = io.silv.ui.theme.LocalSpacing.current
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space.small)
        ) {
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
    val space = io.silv.ui.theme.LocalSpacing.current
    Column(modifier) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Start,
            overflow = TextOverflow.Ellipsis,
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
                maxLines = 2
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
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.DarkGray.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
        )
    }
}