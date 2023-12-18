package io.silv.manga.composables

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.twotone.Archive
import androidx.compose.material.icons.twotone.Unarchive
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import io.silv.common.model.Download
import io.silv.manga.manga_view.DownloadActions
import io.silv.manga.manga_view.MangaViewState
import io.silv.manga.manga_view.NekoDownloadButton
import io.silv.model.SavableChapter
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

@Composable
private fun chapterTitleWithVolText(chapter: SavableChapter, showFullTitle: Boolean) =
    remember(chapter, showFullTitle) {
        if (showFullTitle) {
            "Chapter ${chapter.chapter.coerceAtLeast(0)}"
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
private fun dateWithScanlationText(chapter: SavableChapter) =
    remember(chapter) {
        val pageText =
            if (chapter.lastReadPage > 0 && !chapter.read) {
                "· Page ${chapter.lastReadPage}"
            } else {
                ""
            }
        "${chapter.daysSinceCreatedString} $pageText · ${chapter.scanlationGroupToId?.first ?: chapter.uploader}"
    }


@Composable
private fun ChapterListItem(
    modifier: Modifier = Modifier,
    showFullTitle: Boolean,
    chapter: SavableChapter,
    download: Download?,
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
            val progress by download.progressFlow.collectAsState(initial = 0)

            val status = download.statusFlow.collectAsState().value

            NekoDownloadButton(
                buttonColor = MaterialTheme.colorScheme.primary,
                downloadState = status,
                downloadProgress = progress.toFloat(),
                Modifier.clickable {
                    onCancelClicked(download)
                }
            )
        } else {
            val status = if (chapter.downloaded) Download.State.DOWNLOADED else Download.State.NOT_DOWNLOADED


            NekoDownloadButton(
                buttonColor = MaterialTheme.colorScheme.primary,
                downloadState = status,
                downloadProgress = if (chapter.downloaded) 100f else 0f,
                Modifier.clickable {
                    if (chapter.downloaded)
                        onDeleteClicked()
                    else
                        onDownloadClicked()
                }
            )
        }
    }
}

private fun Modifier.iconButtonBox() = composed {
    minimumInteractiveComponentSize()
        .size(40.0.dp)
        .clip(CircleShape)
        .clickable(
            onClick = {},
            enabled = true,
            role = Role.Button,
            interactionSource = remember { MutableInteractionSource() },
            indication = rememberRipple(
                bounded = false,
                radius = 40.0.dp / 2
            )
        )
}

@Composable
fun AnimatedCircularProgressIndicator(
    currentValue: Int,
    maxValue: Int,
    progressBackgroundColor: Color,
    progressIndicatorColor: Color,
    completedColor: Color,
    modifier: Modifier = Modifier
) {

    val stroke = with(LocalDensity.current) {
        Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {

        val animateFloat = animateFloatAsState(
            targetValue = currentValue.toFloat() / maxValue.toFloat(),
            label = ""
        )


        Canvas(
            Modifier
                .progressSemantics(currentValue / maxValue.toFloat())
                .size(CircularIndicatorDiameter)
        ) {
            // Start at 12 O'clock
            val startAngle = 270f
            val sweep: Float = animateFloat.value * 360f
            val diameterOffset = stroke.width / 2

            drawCircle(
                color = progressBackgroundColor,
                style = stroke,
                radius = size.minDimension / 2.0f - diameterOffset
            )
            drawCircularProgressIndicator(startAngle, sweep, progressIndicatorColor, stroke)

            if (currentValue == maxValue) {
                drawCircle(
                    color = completedColor,
                    style = stroke,
                    radius = size.minDimension / 2.0f - diameterOffset
                )
            }
        }
    }
}

private fun DrawScope.drawCircularProgressIndicator(
    startAngle: Float,
    sweep: Float,
    color: Color,
    stroke: Stroke
) {
    // To draw this circle we need a rect with edges that line up with the midpoint of the stroke.
    // To do this we need to remove half the stroke width from the total diameter for both sides.
    val diameterOffset = stroke.width / 2
    val arcDimen = size.width - 2 * diameterOffset
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(diameterOffset, diameterOffset),
        size = Size(arcDimen, arcDimen),
        style = stroke
    )
}

// Diameter of the indicator circle
private val CircularIndicatorDiameter = 32.0.dp