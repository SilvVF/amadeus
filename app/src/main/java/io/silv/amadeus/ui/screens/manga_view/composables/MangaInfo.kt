package io.silv.amadeus.ui.screens.manga_view.composables

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.silv.amadeus.ui.composables.AnimatedBoxShimmer
import io.silv.amadeus.ui.screens.manga_view.ChapterPageState
import io.silv.amadeus.ui.screens.manga_view.MangaState
import io.silv.amadeus.ui.screens.manga_view.Pagination
import io.silv.amadeus.ui.screens.manga_view.TagsAndLanguages
import io.silv.amadeus.ui.screens.search.LanguageSelection
import io.silv.amadeus.ui.shared.Language
import io.silv.amadeus.ui.shared.noRippleClickable
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.domain.models.SavableChapter
import io.silv.manga.domain.models.SavableManga
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@Composable
fun ChapterVolumeNavBar(
    chaptersShowing: Boolean,
    onChange: (Boolean) -> Unit 
) {
    NavigationBar(
        Modifier.fillMaxWidth(),
        containerColor = Color.Transparent
    ) {
        NavigationBarItem(
            selected = chaptersShowing,
            onClick = { onChange(true) },
            icon = { Icon(imageVector = Icons.Outlined.Article, null) },
            label = { Text("Chapters") }
        )
        NavigationBarItem(
            selected = !chaptersShowing,
            onClick = { onChange(false) },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.LibraryBooks,
                    null
                )
            },
            label = { Text("Volumes") }
        )
    }
}

@Composable
fun MangaContent(
    manga: SavableManga,
    bookmarked: Boolean,
    onBookmarkClicked: (String) -> Unit,
    onTagSelected: (tag: String) -> Unit
) {
    var showMaxLines by rememberSaveable {
        mutableStateOf(false)
    }
    val space = LocalSpacing.current
    Column(Modifier.padding(horizontal = space.med)) {
        MangaActions(
            manga = manga,
            bookmarked = bookmarked,
            onBookmarkClicked = onBookmarkClicked,
        )
        MangaInfo(
            manga = manga,
            showMaxLines = showMaxLines,
            showMaxLinesChange = {
                showMaxLines = it
            },
            onTagSelected = onTagSelected
        )
    }
}

@Composable
fun ChapterListHeader(
    onPageClick: (Int) -> Unit,
    page: Int,
    lastPage: Int,
    sortedAscending: Boolean,
    onChangeDirection: () -> Unit,
    selectedLanguages: List<Language>,
    onLanguageSelected: (Language) -> Unit
) {
    val space = LocalSpacing.current
    Column {
        Pagination(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            page = page,
            lastPage = lastPage,
            onPageClick = onPageClick
        )
        Row(Modifier
            .fillMaxWidth()
            .padding(horizontal = space.med),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LanguageSelection(
                label = {
                    Text("Translated Language")
                },
                placeholder = "All Languages",
                selected = selectedLanguages,
                onLanguageSelected = {
                    onLanguageSelected(it)
                },
            )
            Button(
                shape = RoundedCornerShape(12.dp),
                onClick = onChangeDirection,
                modifier = Modifier.padding(horizontal = space.med)
            ) {
                Text(
                    if (sortedAscending) {
                        "Ascending"
                    } else {
                        "Descending"
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.chapterListItems(
    chapterPageState: ChapterPageState,
    downloadingIds: List<String>,
    onDownloadClicked: (ids: List<String>) -> Unit,
    onDeleteClicked: (id: String) -> Unit,
    onOpenWebView: (url: String) -> Unit,
    onReadClicked: (id: String) -> Unit
) {
    when (chapterPageState) {
        ChapterPageState.Loading -> {
            item {
                ChapterItemPlaceHolder()
            }
        }
        is ChapterPageState.Success -> {
            chapterPageState.volumeToChapters.forEach { (volume, chapters) ->
                stickyHeader {
                    ChapterInfoHeader(
                        chapters = chapters,
                        volume = volume,
                        onDownloadClicked = {
                            onDownloadClicked(chapters.map { it.id })
                        }
                    )
                }
                items(
                    items = chapters,
                    key = { chapter -> chapter.id },
                    contentType = { SavableChapter },
                ) { chapter ->
                    val space = LocalSpacing.current
                    ChapterListItem(
                        modifier = Modifier
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
                        onViewOnWebClicked = {
                            if (chapter.externalUrl != null && chapter.externalUrl?.contains("mangaplus.shueisha") != true) {
                                onOpenWebView(chapter.externalUrl?.replace("\\","") ?: "")
                            }
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
    chapters: List<SavableChapter>,
    volume: Int,
    onDownloadClicked: () -> Unit,
) {
    Surface(
        Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        val space = LocalSpacing.current
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .systemBarsPadding()
                .padding(horizontal = space.large)
        ) {
            val maxToMin = remember(chapters) {
                Pair(
                    chapters.minBy {
                        it.chapter?.trim()?.toDoubleOrNull() ?: Double.MAX_VALUE
                    }.chapter,
                    chapters.maxBy {
                        it.chapter?.toDoubleOrNull() ?: Double.MIN_VALUE
                    }.chapter
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
            Text("Ch. ${maxToMin.second} - ${maxToMin.first}")
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
    onViewOnWebClicked: () -> Unit,
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
                    text = "Chapter ${chapter.chapter ?: "extra"}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(vertical = space.xs)
                )
                Text(
                    text = chapter.title ?: "Ch. ${chapter.chapter}",
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
                if (chapter.externalUrl != null && chapter.externalUrl?.contains("mangaplus.shueisha") != true) {
                    Button(
                        shape = RoundedCornerShape(12.dp),
                        onClick = { onViewOnWebClicked() }
                    ) {
                        Icon(imageVector = Icons.Outlined.Web, contentDescription = null)
                        Text("View on web")
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                    Button(
                        shape = RoundedCornerShape(12.dp),
                        onClick = { onReadClicked() }
                    ) {
                        Icon(imageVector = Icons.Outlined.Article, contentDescription = null)
                        Text("Read")
                    }
                    if (downloading) {
                        CircularProgressIndicator()
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
                                    imageVector = Icons.Filled.Download,
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
                val daysSinceCreation = remember(chapter) {
                    val tz = TimeZone.currentSystemDefault()
                    val text = chapter.createdAt.dropWhile{ it == '0' }.replaceAfter('+', "").dropLast(1)
                    val parsed = LocalDateTime.parse(text).toInstant(tz)
                    val timeNow = Clock.System.now().toLocalDateTime(tz)
                    val days = timeNow.toInstant(tz).minus(parsed).inWholeDays
                    if (days >= 365) {
                        val yearsAgo = days / 365
                        if (yearsAgo <= 1.0) {
                            "last year"
                        } else {
                            "${days/365} years ago"
                        }
                    } else {
                        if (days <= 0.9) {
                            "today"
                        } else {
                            "$days days ago"
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null
                        ,modifier = Modifier.padding(horizontal = space.small)
                    )
                    Text(
                        daysSinceCreation,
                        style = MaterialTheme.typography.labelMedium
                            .copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

fun LazyListScope.volumePosterItems(
    mangaState: MangaState
) {
    when (mangaState) {
        is MangaState.Loading -> item {
            VolumePostersPlaceHolder()
        }
        is MangaState.Success -> {
            items(mangaState.volumeToArt.toList().chunked(2)) {
                val context = LocalContext.current
                val space = LocalSpacing.current
                Row(horizontalArrangement = Arrangement.Center) {
                    it.forEach { (_, url) ->
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(url.ifBlank { mangaState.manga.coverArt }).build(),
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)
                                .height(300.dp)
                                .padding(space.med),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VolumePostersPlaceHolder() {
    FlowRow {
        repeat(4) {
            AnimatedBoxShimmer(modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(200.dp)
            )
        }
    }
}


@Composable
private fun MangaActions(
    manga: SavableManga,
    bookmarked: Boolean,
    onBookmarkClicked: (String) -> Unit
) {
    val space = LocalSpacing.current
    Row(Modifier.fillMaxWidth()) {
        IconButton(
            onClick = { onBookmarkClicked(manga.id) },
            modifier = Modifier.padding(horizontal = space.large)
        ) {
            Icon(
                imageVector = if (bookmarked) {
                    Icons.Filled.Bookmark
                } else {
                    Icons.Outlined.BookmarkBorder
                },
                contentDescription = null
            )
        }
        Button(
            onClick = { },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = space.large)
        ) {
            Icon(
                imageVector = Icons.Outlined.Article,
                contentDescription = null,
                modifier = Modifier.padding(horizontal = space.large)
            )
            Text(text = "Read Now", fontWeight = FontWeight.SemiBold)

        }
    }
}

@Composable
private fun MangaInfo(
    manga: SavableManga,
    showMaxLines: Boolean,
    showMaxLinesChange: (Boolean) -> Unit,
    onTagSelected: (tag: String) -> Unit
) {
    TagsAndLanguages(
        manga = manga,
        navigate = onTagSelected
    )
    Column {
        Text(
            text = "Description",
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = manga.description,
            maxLines = if (showMaxLines) {
                Int.MAX_VALUE
            } else {
                3
            },
            overflow = TextOverflow.Ellipsis
        )
        Row(
            Modifier
                .fillMaxWidth()
                .noRippleClickable {
                    showMaxLinesChange(!showMaxLines)
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (showMaxLines)
                    "show less"
                else
                    "show more",
                style = MaterialTheme.typography.labelSmall
            )
            IconButton(
                onClick = { showMaxLinesChange(!showMaxLines) },
            ) {
                Icon(
                    imageVector = if (showMaxLines)
                        Icons.Outlined.KeyboardArrowUp
                    else
                        Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null
                )
            }
        }
    }
}