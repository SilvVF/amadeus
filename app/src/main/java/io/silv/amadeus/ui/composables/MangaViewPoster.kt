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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import cafe.adriel.voyager.navigator.LocalNavigator
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.silv.amadeus.ui.screens.manga_view.StatsUiState
import io.silv.amadeus.ui.shared.fillMaxAfterMesaure
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.core.filterUnique
import io.silv.manga.domain.models.SavableManga
import io.silv.manga.network.mangadex.models.Status
import java.math.RoundingMode
import java.text.DecimalFormat

@Composable
fun MainPoster(
    manga: SavableManga,
    modifier: Modifier,
    viewMangaArtClick: () -> Unit,
    statsState: StatsUiState,
) {
    val ctx = LocalContext.current
    val space = LocalSpacing.current
    val navigator = LocalNavigator.current

    Box(modifier = modifier) {
        BackgroundImageDarkened(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxAfterMesaure(this, 0.95f)
                .align(TopStart),
            url = manga.coverArt
        )
        Row(
            Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(space.large)
        ) {
            Column(Modifier
                .fillMaxWidth(0.45f)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(ctx)
                        .data(manga.coverArt)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth()
                )
                if (statsState.loading) {
                    AnimatedBoxShimmer(Modifier.size(30.dp))
                } else if (statsState.error != null) {
                    Text(text = statsState.error)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val formated = remember(statsState.data.rating) {
                            val df = DecimalFormat("#.##").apply {
                                roundingMode = RoundingMode.DOWN
                            }
                            df.format(statsState.data.rating)
                        }
                        Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = null,
                            Modifier.padding(space.small)
                        )
                        Text(text = formated,  Modifier.padding(space.small))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Comment,
                            contentDescription = null,
                            Modifier.padding(space.small)
                        )
                        Text(text = statsState.data.comments.toString(),  Modifier.padding(space.small))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Bookmarks,
                            contentDescription = null,
                            Modifier.padding(space.small)
                        )
                        Text(text = statsState.data.follows.toString(),  Modifier.padding(space.small))
                    }
                }
            }
            MangaInfo(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = space.med),
                manga = manga,
                viewMangaArtClick = viewMangaArtClick
            )
        }
    }
}

@Composable
fun MangaInfo(
    modifier: Modifier = Modifier,
    manga: SavableManga,
    viewMangaArtClick: () -> Unit
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
            authors = remember(manga) { (manga.authors + manga.artists).filterUnique { it }.joinToString() }
        )
        PublicationStatusIndicator(status = manga.status, year = manga.year)
        Button(
            shape = RoundedCornerShape(12.dp),
            onClick = viewMangaArtClick,
            modifier = Modifier
                .padding(space.med)
                .align(Alignment.End)
        ) {
            Icon(
                imageVector = Icons.Outlined.Photo,
                contentDescription = null,
                modifier = Modifier.padding(horizontal = space.small)
            )
            Text("Artwork")
        }
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
            .background(Color.Black.copy(alpha = 0.7f))
        )
    }
}