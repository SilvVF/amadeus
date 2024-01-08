package io.silv.manga.composables

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.util.fastFirstOrNull
import io.silv.common.model.Download
import io.silv.manga.manga_view.DownloadActions
import io.silv.manga.manga_view.MangaViewState
import io.silv.ui.composables.ChapterListItem
import io.silv.ui.theme.LocalSpacing
import kotlinx.collections.immutable.ImmutableList
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox

fun LazyListScope.chapterListItems(
    mangaViewState: MangaViewState.Success,
    showFullTitle: Boolean,
    downloads: ImmutableList<Download>,
    onMarkAsRead: (id: String) -> Unit,
    onBookmark: (id: String) -> Unit,
    onDownloadClicked: (id: String) -> Unit,
    onDeleteClicked: (id: String) -> Unit,
    onReadClicked: (id: String) -> Unit,
    downloadActions: DownloadActions,
) {
    items(
        items = mangaViewState.filteredChapters,
        key = { it.id },
    ) { chapter ->
        val space = LocalSpacing.current

        val archive =
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

        val read =
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

        SwipeableActionsBox(
            startActions = listOf(archive),
            endActions = listOf(read),
        ) {
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
                download = downloads.fastFirstOrNull { it.chapter.id == chapter.id },
                showFullTitle = showFullTitle,
                onDownloadClicked = { onDownloadClicked(chapter.id) },
                onDeleteClicked = {
                    onDeleteClicked(chapter.id)
                },
                onCancelClicked = { downloadActions.cancel(it) },
                onPauseClicked = { downloadActions.pause(it) }
            )
        }
    }
}


