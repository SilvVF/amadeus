package io.silv.amadeus.ui.screens.manga_reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ReadMore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.silv.amadeus.ui.composables.MangaViewPoster
import io.silv.amadeus.ui.stateholders.VolumeItemsState
import io.silv.amadeus.ui.stateholders.rememberVolumeItemsState
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.domain.models.DomainChapter
import io.silv.manga.domain.models.DomainManga
import io.silv.manga.local.entity.ProgressState

@Composable
fun ChapterInfoModalDrawer(
    modifier: Modifier = Modifier,
    manga: DomainManga,
    chapter: DomainChapter,
    chapters: List<DomainChapter>,
    onChapterClicked: (DomainChapter) -> Unit,
    onGoToNextChapterClicked: () -> Unit,
    onGoToPrevChapterClicked: () -> Unit,
    content: @Composable () -> Unit
) {
    val space = LocalSpacing.current

    ModalNavigationDrawer(
        drawerContent = {
            Column(
                modifier
                    .clip(
                        RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                    )
                    .background(MaterialTheme.colorScheme.surface),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                MangaViewPoster(
                    Modifier
                        .fillMaxHeight(0.3f)
                        .fillMaxWidth(),
                    includeTopButtons = false,
                    manga = manga,
                )

                Column(Modifier.padding(space.med)) {
                    Button(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text(text = "chapter ${chapter.chapter}")
                    }
                    ChapterNavigationButtons(
                        Modifier.fillMaxWidth(0.8f),
                        goNextClicked = onGoToNextChapterClicked,
                        goPrevClick = onGoToPrevChapterClicked
                    )
                }

                val volumeItemsState = rememberVolumeItemsState(chapters = chapters)

                SortByHeader(volumeItemsState = volumeItemsState)
                ChapterList(
                    modifier = Modifier.weight(1f),
                    volumeItemsState = volumeItemsState,
                    onChapterClicked = {
                        onChapterClicked(it)
                    }
                )
            }
        },
        content = content
    )
}

@Composable
fun SortByHeader(volumeItemsState: VolumeItemsState) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row( verticalAlignment = Alignment.CenterVertically) {
            Text(text = "sorting by: ${
                when (volumeItemsState.sortBy) {
                    VolumeItemsState.SortBy.Asc -> "Ascending"
                    VolumeItemsState.SortBy.Dsc -> "Descending"
                }}",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge
            )
            IconButton(onClick = {
                volumeItemsState.sortByOpposite()
            }) {
                Icon(
                    imageVector = if (volumeItemsState.sortBy == VolumeItemsState.SortBy.Dsc)
                        Icons.Filled.KeyboardArrowDown
                    else
                        Icons.Filled.KeyboardArrowUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
fun ChapterList(
    modifier: Modifier,
    volumeItemsState: VolumeItemsState,
    onChapterClicked: (DomainChapter) -> Unit
) {
    val space = LocalSpacing.current
    when(volumeItemsState.items) {
        is VolumeItemsState.Chapters -> {
            LazyColumn(modifier) {
                items(volumeItemsState.items.items) {
                    ChapterListItem(
                        chapter = it,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChapterClicked(it) }
                            .padding(end = space.med)
                    )
                }
            }
        }
        is VolumeItemsState.Volumes ->  Unit
    }
}

@Composable
private fun ChapterListItem(
    chapter: DomainChapter,
    modifier: Modifier
) {
    val space = LocalSpacing.current
    Row(modifier,
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
            Column(
                Modifier
                    .weight(1f)
                    .padding(space.small)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Filled.FormatAlignLeft,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = space.med),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Chapter #${chapter.chapter}",
                            textAlign = TextAlign.Start,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        chapter.title.takeIf { it?.isNotBlank() ?: false }?.let {
                            Text(
                                text = it,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            Icon(
                imageVector = when(chapter.progress) {
                    ProgressState.Finished -> Icons.Filled.Check
                    ProgressState.NotStarted -> Icons.Filled.ReadMore
                    ProgressState.Reading -> Icons.Filled.ReadMore
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
}

@Composable
fun ChapterNavigationButtons(
    modifier: Modifier,
    goNextClicked: () -> Unit,
    goPrevClick: () -> Unit
) {
    val space = LocalSpacing.current
    Row(
        modifier,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            onClick = { goPrevClick() },
            Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowLeft,
                contentDescription = null
            )
        }
        Spacer(modifier = Modifier.width(space.med))
        Button(
            onClick = { goNextClicked() },
            Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null
            )
        }
    }
}