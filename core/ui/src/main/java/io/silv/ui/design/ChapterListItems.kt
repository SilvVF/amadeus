package io.silv.ui.design

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.twotone.Archive
import androidx.compose.material.icons.twotone.Unarchive
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.util.fastFirstOrNull
import io.silv.common.model.Download
import io.silv.data.chapter.Chapter
import io.silv.data.download.QItem
import io.silv.ui.composables.ChapterListItem
import io.silv.ui.theme.LocalSpacing
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox

fun LazyListScope.chapterListItems(
    items: List<Chapter>,
    showFullTitle: Boolean,
    downloads: List<QItem<Download>>,
    onMarkAsRead: (id: String) -> Unit,
    onBookmark: (id: String) -> Unit,
    onDownloadClicked: (id: String) -> Unit,
    onDeleteClicked: (id: String) -> Unit,
    onReadClicked: (id: String) -> Unit,
    pauseDownload: (download: Download) -> Unit,
    cancelDownload: (download: Download) -> Unit,
) {
    items(
        items = items,
        key = { it.id },
    ) { chapter ->
        val space = LocalSpacing.current
        SwipeableActionsBox(
            startActions = listOf(
                SwipeAction(
                    icon =
                        rememberVectorPainter(
                            if (chapter.bookmarked) {
                                Icons.TwoTone.Archive
                            } else {
                                Icons.TwoTone.Unarchive
                            },
                        ),
                    background = MaterialTheme.colorScheme.primary,
                    isUndo = chapter.bookmarked,
                    onSwipe = {
                        onBookmark(chapter.id)
                    },
                )
            ),
            endActions = listOf(
                SwipeAction(
                    icon =
                        rememberVectorPainter(
                            if (chapter.read) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.VisibilityOff
                            },
                        ),
                    background = MaterialTheme.colorScheme.primary,
                    isUndo = chapter.read,
                    onSwipe = {
                        onMarkAsRead(chapter.id)
                    },
                )
            ),
        ) {
            val download by remember(downloads) {
                derivedStateOf { downloads.fastFirstOrNull { it.data.chapter.id == chapter.id } }
            }

            ChapterListItem(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onReadClicked(chapter.id) }
                        .padding(
                            vertical = space.med,
                            horizontal = space.large,
                        ),
                chapter = chapter,
                download = download,
                showFullTitle = showFullTitle,
                onDownloadClicked = { onDownloadClicked(chapter.id) },
                onDeleteClicked = {
                    onDeleteClicked(chapter.id)
                },
                onCancelClicked = { cancelDownload(it) },
                onPauseClicked = { pauseDownload(it) }
            )
        }
    }
}