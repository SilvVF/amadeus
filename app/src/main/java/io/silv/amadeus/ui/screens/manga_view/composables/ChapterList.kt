package io.silv.amadeus.ui.screens.manga_view.composables

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DismissDirection.*
import androidx.compose.material3.DismissState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import io.silv.amadeus.ui.composables.AnimatedBoxShimmer
import io.silv.amadeus.ui.screens.manga_view.MangaViewState
import io.silv.amadeus.ui.shared.CenterBox
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.domain.models.SavableChapter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Reset(dismissState: DismissState, action: () -> Unit) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(key1 = dismissState.dismissDirection) {
        scope.launch {
            dismissState.reset()
            action()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
fun LazyListScope.chapterListItems(
    mangaViewState: MangaViewState,
    downloadingIds: List<Pair<String, Float>>,
    onMarkAsRead: (id: String) -> Unit,
    onBookmark: (id: String) -> Unit,
    onDownloadClicked: (ids: List<String>) -> Unit,
    onDeleteClicked: (id: String) -> Unit,
    onReadClicked: (id: String) -> Unit
) {
    when (mangaViewState) {
        is MangaViewState.Loading -> {
            item { ChapterItemPlaceHolder() }
        }
        is MangaViewState.Success -> {
            items(
                items = mangaViewState.chapters,
                key = { it.id }
            ) { chapter ->
                val space = LocalSpacing.current
                val dismissState = rememberDismissState()
                when  {
                    dismissState.isDismissed(EndToStart) ->
                        Reset(dismissState = dismissState) {
                            onMarkAsRead(chapter.id)
                        }
                    dismissState.isDismissed(StartToEnd) ->
                        Reset(dismissState = dismissState){
                            onBookmark(chapter.id)
                        }
                }
                SwipeToDismiss(
                    state = dismissState,
                    background = {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(Modifier
                                .fillMaxWidth()
                                .padding(horizontal = space.large)
                            ) {
                                when (dismissState.dismissDirection) {
                                    StartToEnd ->  Column(
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.align(Alignment.CenterStart)
                                    ) {
                                        Icon(
                                            imageVector = if(chapter.bookmarked)
                                                Icons.Default.BookmarkRemove
                                            else Icons.Default.BookmarkAdd,
                                            contentDescription = "bookmark"
                                        )
                                        Text(if(chapter.bookmarked) "Remove bookmark" else "Add bookmark")
                                    }
                                    EndToStart ->  Column(
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.align(Alignment.CenterEnd)
                                    ) {
                                        Icon(
                                            imageVector = if(chapter.read)
                                                Icons.Default.VisibilityOff
                                            else Icons.Default.Visibility,
                                            contentDescription = "read"
                                        )
                                        Text(if(chapter.read) "Mark unread" else "Mark read")
                                    }
                                    else -> Unit
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .animateItemPlacement(),
                    dismissContent = {
                        ChapterListItem(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.background)
                                .fillMaxWidth()
                                .padding(
                                    vertical = space.med,
                                    horizontal = space.large
                                ),
                            chapter = chapter,
                            downloadProgress = downloadingIds.fastFirstOrNull { it.first == chapter.id }?.second,
                            onDownloadClicked = {
                                onDownloadClicked(listOf(chapter.id))
                            },
                            onDeleteClicked = {
                                onDeleteClicked(chapter.id)
                            },
                            onReadClicked = {
                                onReadClicked(chapter.id)
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ChapterItemPlaceHolder() {
    val space = LocalSpacing.current
    Column {
        AnimatedBoxShimmer(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = space.med)
        )
        repeat(4) {
            AnimatedBoxShimmer(
                Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(space.med)
            )
        }
    }
}

@Composable
private fun ChapterListItem(
    modifier: Modifier = Modifier,
    chapter: SavableChapter,
    downloadProgress: Float?,
    onDownloadClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onReadClicked: () -> Unit,
) {
    val space = LocalSpacing.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val chapterTitleWithVolText = remember(chapter) {
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
                .clickable { onReadClicked() }
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
        if (downloadProgress != null) {
            CenterBox {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = null
                )
                CircularProgressIndicator()
            }
        } else {
            if (chapter.downloaded) {
                IconButton(onClick = { onDeleteClicked() }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null
                    )
                }
            } else {
                IconButton(onClick = { onDownloadClicked() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowCircleDown,
                        contentDescription = null
                    )
                }
            }
        }
    }
}