package io.silv.manga.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.TopStart
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.silv.common.filterUnique
import io.silv.common.model.ReadingStatus
import io.silv.common.model.Status
import io.silv.domain.manga.model.Manga
import io.silv.manga.view.MangaStats
import io.silv.manga.view.StatsUiState
import io.silv.ui.fillMaxSizeAfterMeasure
import io.silv.ui.theme.LocalSpacing

@Composable
fun MangaImageWithTitle(
    modifier: Modifier = Modifier,
    manga: Manga,
    stats: StatsUiState,
    padding: PaddingValues,
    showChapterArt: () -> Unit,
    addToLibrary: (String) -> Unit,
    viewOnWeb: () -> Unit,
    changeStatus: (ReadingStatus) -> Unit
) {
    val space = LocalSpacing.current

    Box(modifier = modifier) {
        BackgroundImageDarkened(
            modifier =
            Modifier
                .fillMaxWidth()
                .fillMaxSizeAfterMeasure(1f)
                .align(TopStart),
            manga = manga,
        )
        Column(
            Modifier.padding(horizontal = space.med)
        ) {
            Spacer(modifier = Modifier.height(space.xlarge))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(padding)
                    .padding(space.med),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Bottom
            ) {
                MangaTitle(
                    modifier =
                    Modifier
                        .fillMaxWidth(0.9f),
                    status = manga.status,
                    year = manga.year,
                    title = manga.titleEnglish,
                    altTitle = manga.alternateTitles["ja-ro"] ?: "",
                    authors = remember(manga) { (manga.authors + manga.artists).filterUnique { it }.joinToString() },
                )
            }
            MangaStats(
                modifier = Modifier.fillMaxWidth(),
                state = stats,
            )
            MangaActions(
                modifier = Modifier.fillMaxWidth(),
                inLibrary = manga.inLibrary,
                showChapterArt = showChapterArt,
                addToLibraryClicked = {
                   addToLibrary(manga.id)
                },
                viewOnWebClicked = viewOnWeb,
                changeStatus = changeStatus,
                readingStatus = manga.readingStatus
            )
        }
    }
}

@Composable
private fun PublicationStatusIndicator(
    modifier: Modifier = Modifier,
    status: Status,
    year: Int?,
) {
    val space = LocalSpacing.current
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier =
            Modifier
                .padding(space.small)
                .size(8.dp)
                .clip(CircleShape)
                .drawWithCache {
                    onDrawBehind {
                        drawRect(
                            color =
                            when (status) {
                                Status.completed -> Color.Cyan
                                Status.ongoing -> Color.Green
                                Status.cancelled -> Color.Red
                                Status.hiatus -> Color.Yellow
                            },
                        )
                    }
                },
        )
        year?.let {
            Text(
                text = "Pub: $year;",
                modifier = Modifier.padding(horizontal = space.small),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Text(
            text = "Status, ${status.name}",
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun MangaTitle(
    modifier: Modifier = Modifier,
    title: String,
    altTitle: String,
    authors: String,
    status: Status,
    year: Int?,
) {
    val space = LocalSpacing.current
    Column(
        modifier,
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Start,
            overflow = TextOverflow.Ellipsis,
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = space.med),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = altTitle,
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
            )
            Text(
                text = authors,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Start,
            )
        }
        PublicationStatusIndicator(
            modifier = Modifier.fillMaxWidth(),
            status = status,
            year = year,
        )
    }
}

@Composable
private fun BackgroundImageDarkened(
    modifier: Modifier,
    manga: Manga,
    background: Color = MaterialTheme.colorScheme.background,
) {
    Box(
        modifier =
        modifier
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    drawRect(
                        brush =
                        Brush.verticalGradient(
                            colors =
                            listOf(
                                Color.DarkGray.copy(alpha = 0.6f),
                                background,
                            ),
                        ),
                    )
                }
            },
    ) {
        AsyncImage(
            model = manga,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
