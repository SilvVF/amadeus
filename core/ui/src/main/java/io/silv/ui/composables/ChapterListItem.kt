package io.silv.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.silv.common.model.Download
import io.silv.data.download.QItem
import io.silv.domain.chapter.model.Chapter
import io.silv.ui.theme.LocalSpacing

@Composable
private fun chapterTitleWithVolText(chapter: Chapter, showFullTitle: Boolean) =
    remember(chapter, showFullTitle) {
        if (showFullTitle) {
            "Chapter ${chapter.chapter.coerceAtLeast(0.0)}"
        }
        val vol =
            if (chapter.volume >= 0) {
                "Vol. ${chapter.volume}"
            } else {
                ""
            }
        "$vol Ch. ${if (chapter.validNumber) chapter.chapter else "extra"} - " + chapter.title
    }

@Composable
private fun dateWithScanlationText(chapter: Chapter) =
    remember(chapter) {
        val pageText =
            if ((chapter.lastReadPage ?: 0) > 0 && !chapter.read) {
                "· Page ${chapter.lastReadPage}"
            } else {
                ""
            }
        "${chapter.daysSinceCreatedString} $pageText · ${chapter.scanlationGroupToId?.first ?: chapter.uploader}"
    }

@Composable
fun ChapterListItem(
    modifier: Modifier = Modifier,
    showFullTitle: Boolean,
    chapter: Chapter,
    download: QItem<Download>?,
    onDownloadClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onPauseClicked: (download: Download) -> Unit,
    onCancelClicked: (download: Download) -> Unit,
) {
    val space = LocalSpacing.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            Modifier
                .padding(space.med)
                .weight(1f),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (chapter.bookmarked) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = "bookmarked",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = space.xs),
                    )
                }
                Text(
                    text = chapterTitleWithVolText(chapter = chapter, showFullTitle = showFullTitle),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style =
                    MaterialTheme.typography.bodyMedium.copy(
                        color =
                        if (!chapter.read) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            Color.DarkGray
                        },
                    ),
                )
            }
            Spacer(modifier = Modifier.height(space.small))
            Text(
                text = dateWithScanlationText(chapter = chapter),
                style =
                MaterialTheme.typography.labelLarge.copy(
                    color =
                    if (!chapter.read) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        Color.DarkGray
                    },
                ),
            )
        }

        if (download != null) {
            val status by download.statusFlow.collectAsStateWithLifecycle(QItem.State.IDLE)
            val progress by download.data.progressFlow.collectAsStateWithLifecycle(0)

            ChapterDownloadIndicator(
                enabled = true,
                downloadStateProvider = { status },
                downloadProgressProvider = { progress },
                onClick = { action ->
                    when (action) {
                        ChapterDownloadAction.START -> onDownloadClicked()
                        ChapterDownloadAction.START_NOW -> Unit
                        ChapterDownloadAction.CANCEL -> onCancelClicked(download.data)
                        ChapterDownloadAction.DELETE -> onDeleteClicked()
                    }
                }
            )
        } else {
            ChapterDownloadIndicator(
                enabled = true,
                downloadStateProvider = {
                    if (chapter.downloaded) {
                        QItem.State.COMPLETED
                    } else {
                        QItem.State.IDLE
                    }
                 },
                downloadProgressProvider = { 0 },
                onClick = { action ->
                    when (action) {
                        ChapterDownloadAction.START -> onDownloadClicked()
                        ChapterDownloadAction.START_NOW -> Unit
                        ChapterDownloadAction.CANCEL -> Unit
                        ChapterDownloadAction.DELETE -> onDeleteClicked()
                    }
                }
            )
        }
    }
}