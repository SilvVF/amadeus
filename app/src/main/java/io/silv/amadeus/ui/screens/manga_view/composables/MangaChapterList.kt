package io.silv.amadeus.ui.screens.manga_view.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.silv.amadeus.ui.stateholders.SortedChapters
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.domain.models.SavableChapter


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChapterList(
    chapters: List<SavableChapter>,
    sortBy: SortedChapters.SortBy,
    downloads: List<String>,
    sortByChange: () -> Unit,
    readButtonClick: (SavableChapter) -> Unit,
    downloadChapterClicked: (SavableChapter) -> Unit,
    deleteChapterClicked: (SavableChapter) -> Unit
) {
    val space = LocalSpacing.current
    LazyColumn(Modifier.fillMaxSize()) {
        stickyHeader {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Language: ${chapters.firstOrNull()?.translatedLanguage}",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "sorting by: ${
                        when (sortBy) {
                            SortedChapters.SortBy.Asc -> "Ascending"
                            SortedChapters.SortBy.Dsc -> "Descending"
                        }}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    IconButton(onClick = sortByChange) {
                        Icon(
                            imageVector = if (sortBy == SortedChapters.SortBy.Dsc)
                                Icons.Filled.KeyboardArrowDown
                            else
                                Icons.Filled.KeyboardArrowUp,
                            contentDescription = null
                        )
                    }
                }
            }
        }
        items(
            chapters,
            key = { item -> item.id }
        ){ chapter ->
            ChapterListItem(
                chapter,
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = space.med),
                downloading =  chapter.id in downloads,
                downloadChapterClicked = {
                    downloadChapterClicked(chapter)
                },
                readButtonClick = { readButtonClick(chapter) },
                deleteChapterClicked = { deleteChapterClicked(chapter) }
            )
            Divider()
        }
    }
}

@Composable
private fun ChapterListItem(
    chapter: SavableChapter,
    modifier: Modifier = Modifier,
    downloading: Boolean,
    readButtonClick: () -> Unit,
    downloadChapterClicked: () -> Unit,
    deleteChapterClicked: () -> Unit
) {
    val space = LocalSpacing.current
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier.clickable { expanded = !expanded }) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(imageVector = Icons.Filled.FormatAlignLeft, contentDescription = null)
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = space.med),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Chapter #${chapter.chapter}",
                    textAlign = TextAlign.Start
                )
                chapter.title.takeIf { it?.isNotBlank() ?: false }?.let {
                    Text(text = it)
                }
            }
            if (downloading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(horizontal = space.med)
                        .size(24.dp),
                    strokeWidth = 4.dp,
                )
            } else {
                IconButton(
                    onClick = if (chapter.downloaded) {
                        deleteChapterClicked
                    } else {
                        downloadChapterClicked
                    }
                ) {
                    Icon(
                        imageVector = if (chapter.downloaded) {
                            Icons.Filled.Delete
                        } else {
                            Icons.Filled.Download
                        },
                        contentDescription = null
                    )
                }
            }
            Button(onClick = readButtonClick) {
                Text(text = "Read")
            }
        }
        AnimatedVisibility(visible = expanded) {
            val date = remember(chapter) {
                chapter.createdAt.split("-", "T")
            }
            Column {
                Text(text = "Pages - ${chapter.pages}")
                Text(text = "created at ${date.getOrNull(1)}/${date.getOrNull(2)}/${date.getOrNull(0)}")
                Text(text = "version - ${chapter.version}")
            }
        }
    }
}