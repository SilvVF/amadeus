package io.silv.reader.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.silv.ui.theme.LocalSpacing
import io.silv.model.SavableChapter

@Composable
fun ChaptersList(
    modifier: Modifier = Modifier,
    selected: SavableChapter,
    chapters: List<SavableChapter>,
    onChapterClicked: (id: String) -> Unit,
    onBookmarkClick: (chapterId: String) -> Unit
) {
    val space = io.silv.ui.theme.LocalSpacing.current
    LazyColumn(modifier = modifier) {
        items(
            items = chapters,
            key = { chapter -> chapter.id }
        ) { chapter ->
            val volumeAndChapter = remember(chapter) {
                "Vol.${chapter.volume} Ch.${chapter.chapter} - ${chapter.title}"
            }
            val chapterInfoText = remember(chapter) {
                "${chapter.createdAt.month.name} ${chapter.createdAt.dayOfMonth}, " +
                        "${chapter.createdAt.year} * ${chapter.scanlationGroupToId?.first}"
            }
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onChapterClicked(chapter.id) }
                        .padding(space.med),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val textColor = if (chapter.id == selected.id) MaterialTheme.colorScheme.primary else Color.Unspecified
                    Column {
                        Text(text = volumeAndChapter, color = textColor)
                        Text(text = chapterInfoText, color = textColor)
                    }
                    IconButton(onClick = {
                        onBookmarkClick(chapter.id)
                    }) {
                        Icon(
                            imageVector = if(chapter.bookmarked)
                                Icons.Filled.Bookmark
                            else
                                Icons.Filled.BookmarkBorder,
                            contentDescription = null
                        )
                    }
                }
                Divider()
            }
        }
    }
}