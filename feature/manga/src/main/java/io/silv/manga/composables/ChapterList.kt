package io.silv.manga.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.twotone.Archive
import androidx.compose.material.icons.twotone.Unarchive
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastAny
import io.silv.manga.manga_view.MangaViewState
import io.silv.model.SavableChapter
import io.silv.ui.CenterBox
import io.silv.ui.theme.LocalSpacing
import kotlinx.collections.immutable.ImmutableList
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox

fun LazyListScope.chapterListItems(
    mangaViewState: MangaViewState.Success,
    showFullTitle: Boolean,
    downloadingIds: ImmutableList<String>,
    onMarkAsRead: (id: String) -> Unit,
    onBookmark: (id: String) -> Unit,
    onDownloadClicked: (id: String) -> Unit,
    onDeleteClicked: (id: String) -> Unit,
    onReadClicked: (id: String) -> Unit
) {
    items(
        items = mangaViewState.filteredChapters,
        key = { it.id }
    ) { chapter ->
        val space = LocalSpacing.current

        val archive = SwipeAction(
            icon = rememberVectorPainter(
                if(chapter.bookmarked) { Icons.TwoTone.Archive }
                else { Icons.TwoTone.Unarchive }
            ),
            background = MaterialTheme.colorScheme.primary,
            isUndo = chapter.bookmarked,
            onSwipe = {
                onBookmark(chapter.id)
            }

        )

        val read = SwipeAction(
            icon = rememberVectorPainter(
                if (chapter.read) { Icons.Filled.VisibilityOff }
                else { Icons.Filled.VisibilityOff }
            ),
            background = MaterialTheme.colorScheme.primary,
            isUndo = chapter.read,
            onSwipe = {
                onMarkAsRead(chapter.id)
            }
        )

        SwipeableActionsBox(
            startActions = listOf(archive),
            endActions = listOf(read)
        ) {
            ChapterListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onReadClicked(chapter.id) }
                    .padding(
                        vertical = space.med,
                        horizontal = space.large
                    ),
                chapter = chapter,
                downloading = downloadingIds.fastAny { it == chapter.id },
                showFullTitle = showFullTitle,
                onDownloadClicked = { onDownloadClicked(chapter.id) },
                onDeleteClicked = {
                    onDeleteClicked(chapter.id)
                }
            )
        }
    }
}

@Composable
private fun ChapterListItem(
    modifier: Modifier = Modifier,
    showFullTitle: Boolean,
    chapter: SavableChapter,
    downloading: Boolean,
    onDownloadClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
) {
    val space = LocalSpacing.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val chapterTitleWithVolText = remember(chapter, showFullTitle) {
            if (showFullTitle) { "Chapter ${chapter.chapter.coerceAtLeast(0)}" }
            val vol = if (chapter.volume >= 0) { "Vol. ${chapter.volume}"} else ""
            "$vol Ch. ${if(chapter.validNumber) chapter.chapter else "extra"} - " + chapter.title
        }
        val dateWithScanlationText = remember(chapter) {
            val pageText = if (chapter.lastReadPage > 0 && !chapter.read) { "· Page ${chapter.lastReadPage}" } else { "" }
            "${chapter.daysSinceCreatedString} $pageText · ${chapter.scanlationGroupToId?.first ?: chapter.uploader}"
        }
        Column(
            Modifier
                .padding(space.med)
                .weight(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (chapter.bookmarked) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = "bookmarked",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = space.xs)
                    )
                }
                Text(
                    text = chapterTitleWithVolText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if(!chapter.read)
                            MaterialTheme.colorScheme.onBackground
                        else
                            Color.DarkGray
                    )
                )
            }
            Spacer(modifier = Modifier.height(space.small))
            Text(
                text = dateWithScanlationText,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = if(!chapter.read)
                        MaterialTheme.colorScheme.onBackground
                    else
                        Color.DarkGray
                )
            )
        }
        when {
            downloading -> CenterBox {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = null
                )
                CircularProgressIndicator()
            }
           chapter.ableToDownload -> {
               val (icon, action) = if(chapter.downloaded)
                   Icons.Filled.DeleteOutline to onDeleteClicked
               else
                   Icons.Filled.ArrowCircleDown to onDownloadClicked

               IconButton(onClick = action) {
                   Icon(
                       imageVector = icon,
                       contentDescription = null
                   )
               }
            }
        }
    }
}