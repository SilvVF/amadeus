package io.silv.amadeus.ui.screens.manga_view.composables

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Downloading
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import io.silv.amadeus.ui.composables.AnimatedBoxShimmer
import io.silv.amadeus.ui.screens.manga_view.MangaViewState
import io.silv.amadeus.ui.shared.CenterBox
import io.silv.amadeus.ui.shared.Language
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.domain.models.SavableChapter

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.chapterListItems(
    mangaViewState: MangaViewState,
    asc: Boolean,
    downloadingIds: List<String>,
    onDownloadClicked: (ids: List<String>) -> Unit,
    onDeleteClicked: (id: String) -> Unit,
    onReadClicked: (id: String) -> Unit
) {
    when (mangaViewState) {
        is MangaViewState.Loading -> {
            item {
                ChapterItemPlaceHolder()
            }
        }
        is MangaViewState.Success -> {
            mangaViewState.volumeToChapters.fastForEach { (volume, chapters) ->
                item {
                    ChapterInfoHeader(
                        chapters = chapters,
                        volume = volume,
                        onDownloadClicked = {
                            onDownloadClicked(chapters.map { it.id })
                        },
                        modifier = Modifier.fillMaxWidth(),
                        asc = asc
                    )
                }
                items(
                    items = chapters,
                    key = { chapter -> chapter.id },
                ) { chapter ->
                    val space = LocalSpacing.current
                    ChapterListItem(
                        modifier = Modifier
                            .animateItemPlacement()
                            .padding(
                                vertical = space.med,
                                horizontal = space.large
                            )
                            .fillMaxWidth(),
                        chapter = chapter,
                        downloading = chapter.id in downloadingIds,
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
                .padding(space.med)
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
private fun ChapterInfoHeader(
    modifier: Modifier,
    chapters: List<SavableChapter>,
    volume: Int,
    asc: Boolean,
    onDownloadClicked: () -> Unit,
) {
    Surface(
        modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        val space = LocalSpacing.current
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = space.large)
        ) {
            val maxToMin = remember(chapters) {
                val valid = chapters.filter { it.validNumber }
                Pair(
                    valid.minByOrNull { it.chapter }?.chapter ?: 0,
                    valid.maxByOrNull { it.chapter }?.chapter ?: 0
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Volume ${if (volume <= 0) "no volume" else volume}")
                IconButton(onClick = onDownloadClicked) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = null
                    )
                }
            }
            Text(
                "Ch. ${if (asc) maxToMin.first else maxToMin.second} -" +
                    " ${if (asc) maxToMin.second else maxToMin.first}"
            )
        }
    }
}

@Composable
private fun ChapterListItem(
    modifier: Modifier = Modifier,
    chapter: SavableChapter,
    downloading: Boolean,
    onDownloadClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onReadClicked: () -> Unit,
) {
    val space = LocalSpacing.current
    Card(modifier) {
        Row(
            Modifier
                .padding(space.med)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.fillMaxWidth(0.5f)) {
                Text(
                    text = "Chapter ${chapter.chapter.takeIf { chapter.validNumber } ?: "extra"}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(vertical = space.xs)
                )
                Text(
                    text = chapter.title.ifBlank { "Ch. ${chapter.chapter}" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(vertical = space.xs)
                )
                Row {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Group,
                                contentDescription = null,
                                modifier = Modifier.padding(horizontal = space.xs)
                            )
                            Text(chapter.scanlationGroupToId?.first ?: "")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.Person, contentDescription = null)
                            Text(chapter.userToId?.first ?: "")
                        }
                    }
                }
            }
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                    Button(
                        shape = RoundedCornerShape(12.dp),
                        onClick = { onReadClicked() }
                    ) {
                        Icon(imageVector = Icons.Outlined.Article, contentDescription = null)
                        Text("Read")
                    }
                    if (downloading) {
                        CenterBox {
                            IconButton(onClick = onDownloadClicked) {
                                Icon(
                                    imageVector = Icons.Outlined.Downloading,
                                    contentDescription = null
                                )
                            }
                            CircularProgressIndicator()
                        }
                    } else {
                        if (chapter.downloaded) {
                            IconButton(onClick = onDeleteClicked) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = null
                                )
                            }
                        } else {
                            IconButton(onClick = onDownloadClicked) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowCircleDown,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
                Row(modifier = Modifier.padding(vertical = space.small), verticalAlignment = Alignment.CenterVertically) {
                    val language = remember(chapter) {
                        Language.values().find { it.code == chapter.translatedLanguage }
                    }
                    Text(text = language?.string ?: chapter.translatedLanguage , modifier = Modifier.padding(horizontal = space.small))
                    if (language != null) {
                        Image(
                            painter = painterResource(id = language.resId),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null
                        ,modifier = Modifier.padding(horizontal = space.small)
                    )
                    Text(
                        text = chapter.daysSinceCreatedString,
                        style = MaterialTheme.typography.labelMedium
                            .copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}