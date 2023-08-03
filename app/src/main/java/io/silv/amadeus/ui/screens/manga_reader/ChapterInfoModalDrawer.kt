package io.silv.amadeus.ui.screens.manga_reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.silv.amadeus.ui.stateholders.SortedChapters
import io.silv.amadeus.ui.stateholders.rememberSortedChapters
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.domain.models.SavableChapter
import io.silv.manga.domain.models.SavableManga
import io.silv.manga.local.entity.ProgressState

@Composable
fun ChapterInfoModalDrawer(
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
    manga: SavableManga,
    chapter: SavableChapter,
    chapters: List<SavableChapter>,
    onChapterClicked: (SavableChapter) -> Unit,
    onGoToNextChapterClicked: (SavableChapter) -> Unit,
    onGoToPrevChapterClicked: (SavableChapter) -> Unit,
    content: @Composable () -> Unit
) {
    val space = LocalSpacing.current

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            if (drawerState.isClosed) {
                return@ModalNavigationDrawer
            }
            Column(
                modifier
                    .clip(
                        RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                    )
                    .background(MaterialTheme.colorScheme.surface),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Column(Modifier.padding(space.med)) {
                    Button(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text(text = "chapter ${chapter.chapter}")
                    }
                    ChapterNavigationButtons(
                        Modifier.fillMaxWidth(0.8f),
                        goNextClicked = {
                           onGoToNextChapterClicked(chapter)
                        },
                        goPrevClick = {
                            onGoToPrevChapterClicked(chapter)
                        }
                    )
                }

                val sortedChapters = rememberSortedChapters(chapters = chapters)

                SortByHeader(sortedChapters = sortedChapters)
                ChapterList(
                    modifier = Modifier.weight(1f),
                    sortedChapters = sortedChapters,
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
fun SortByHeader(sortedChapters: SortedChapters) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row( verticalAlignment = Alignment.CenterVertically) {
            Text(text = "sorting by: ${
                when (sortedChapters.sortBy) {
                    SortedChapters.SortBy.Asc -> "Ascending"
                    SortedChapters.SortBy.Dsc -> "Descending"
                }}",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge
            )
            IconButton(onClick = {
                sortedChapters.sortByOpposite()
            }) {
                Icon(
                    imageVector = if (sortedChapters.sortBy == SortedChapters.SortBy.Dsc)
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
    sortedChapters: SortedChapters,
    onChapterClicked: (SavableChapter) -> Unit
) {
    val space = LocalSpacing.current
    LazyColumn(modifier) {
        items(sortedChapters.sortedChapters) {
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

@Composable
private fun ChapterListItem(
    chapter: SavableChapter,
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