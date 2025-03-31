package io.silv.reader.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.twotone.Archive
import androidx.compose.material.icons.twotone.Unarchive
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import io.silv.common.model.Download
import io.silv.datastore.ReaderPrefs
import io.silv.datastore.collectAsState
import io.silv.domain.chapter.model.Chapter
import io.silv.domain.manga.model.Manga
import io.silv.reader.loader.ReaderChapter
import io.silv.ui.Converters
import io.silv.ui.ReaderLayout
import io.silv.ui.composables.ChapterDownloadAction
import io.silv.ui.composables.ChapterDownloadIndicator
import io.silv.ui.layout.DragAnchors
import io.silv.ui.layout.ExpandableInfoLayout
import io.silv.ui.layout.ExpandableScope
import io.silv.ui.layout.rememberExpandableState
import io.silv.ui.theme.LocalSpacing
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import kotlin.math.roundToInt

private enum class MenuTabs(val icon: ImageVector) {
    Chapters(Icons.Filled.FormatListNumbered), Options(Icons.Filled.Tune)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReaderMenuOverlay(
    readerChapter: () -> ReaderChapter,
    manga: () -> Manga,
    chapters: () -> ImmutableList<Chapter>,
    menuVisible: () -> Boolean,
    currentPage: () -> Int,
    layoutDirection: LayoutDirection,
    onDismissRequested: () -> Unit,
    loadPrevChapter: () -> Unit,
    loadNextChapter: () -> Unit,
    changePage: (page: Int) -> Unit,
    onBackArrowClick: () -> Unit,
    onViewOnWebClick: () -> Unit,
    chapterActions: ChapterActions,
    content: @Composable () -> Unit,
) {
    val space = LocalSpacing.current
    val expandableState = rememberExpandableState(startProgress = DragAnchors.End)

    LaunchedEffect(Unit) {
        snapshotFlow { expandableState.fraction.value }.collect {
            if (it == 1f) {
                onDismissRequested()
            }
        }
    }

    LaunchedEffect(menuVisible()) {
        when {
            !menuVisible() -> expandableState.hide()
            menuVisible() && expandableState.isHidden -> expandableState.show()
        }
    }

    val tabs = remember {
        MenuTabs.values().toList().toImmutableList()
    }

    val menuPagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()

    Box(
        Modifier.fillMaxSize()
    ) {
        content()
        AnimatedVisibility(
            visible = menuVisible(),
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it }
        ) {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackArrowClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onViewOnWebClick) {
                        Icon(
                            imageVector = Icons.Default.Web,
                            contentDescription = null
                        )
                    }
                },
                title = {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val chapter = readerChapter()
                        val chapterText = remember(chapter) {
                            if (chapter.chapter.validNumber) {
                                "Ch.${chapter.chapter.chapter} - ${chapter.chapter.title}"
                            } else {
                                chapter.chapter.title
                            }
                        }
                        Text(manga().titleEnglish)
                        Text(
                            chapterText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .78f)
                            )
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            )
        }
        ExpandableInfoLayout(
            modifier = Modifier.align(Alignment.BottomCenter),
            state = expandableState,
            peekContent = {
                Box {
                    MenuPageSlider(
                        modifier = Modifier
                            .padding(space.large)
                            .align(Alignment.Center)
                            .consumeWindowInsets(WindowInsets.systemBars),
                        fractionProvider = { expandableState.fraction.value },
                        pageIdxProvider =  currentPage,
                        pageCountProvider = { readerChapter().pages?.size ?: 0 },
                        layoutDirectionProvider = { layoutDirection },
                        onPrevClick = loadPrevChapter,
                        onNextClick = loadNextChapter,
                        onPageChange = changePage
                    )
                    TabRow(
                        selectedTabIndex = menuPagerState.currentPage,
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .wrapContentHeight()
                            .layout { measurable, constraints ->

                                val placeable = measurable.measure(constraints)

                                val height = placeable.height * (1 - expandableState.fraction.value)

                                layout(placeable.width, height.roundToInt()) {
                                    placeable.placeRelative(0, 0)
                                }
                            }
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    ) {
                        tabs.fastForEach { menuTab ->
                            Tab(
                                selected = menuPagerState.currentPage == menuTab.ordinal,
                                text = { Text(menuTab.toString()) },
                                icon = { Icon(menuTab.icon, null) },
                                onClick = {
                                    scope.launch {
                                        menuPagerState.animateScrollToPage(menuTab.ordinal)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            ) {
                HorizontalPager(
                    state = menuPagerState,
                    beyondViewportPageCount = 0,
                    modifier = Modifier
                        .animateContentSize()
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    when(it) {
                        MenuTabs.Chapters.ordinal -> ChapterList(
                            chapters = chapters,
                            modifier = Modifier
                                .fillMaxHeight(0.6f)
                                .fillMaxWidth(),
                            actions = chapterActions
                        )
                        MenuTabs.Options.ordinal -> ReaderOptions(
                            modifier = Modifier
                                .fillMaxHeight(0.6f)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun ToggleItem(
    modifier: Modifier,
    text: String,
    enabled: () -> Boolean,
) {
    Row(
        modifier= modifier
    ) {
        Text(text)

    }
}

@Stable
data class ChapterActions(
    val delete: (chapterId: String) -> Unit ,
    val markRead: (chapterId: String) -> Unit,
    val bookmark: (chapterId: String) -> Unit,
    val cancelDownload: (download: Download) -> Unit,
    val pauseDownloads: () -> Unit,
    val download: (chapterId: String) -> Unit,

)

@Composable
private fun ExpandableScope.ChapterList(
    modifier: Modifier = Modifier,
    chapters: () -> ImmutableList<Chapter>,
    actions: ChapterActions,
) {
    val lazyListState = rememberLazyListState()
    LazyColumn(
        state = lazyListState,
        modifier = modifier.nestedScroll(lazyListState)
    ) {
        chapterListItems(
            chapters,
            onReadClicked = { actions.markRead(it) },
            onDeleteClicked = { actions.delete(it) },
            downloadsProvider =  { persistentListOf() },
            showFullTitle = true,
            onMarkAsRead = { actions.markRead(it) },
            onBookmark = { actions.bookmark(it) },
            onDownloadClicked = { actions.download(it) },
            onCancelClicked = { actions.cancelDownload(it) },
            onPauseClicked = { actions.pauseDownloads() }
        )
    }
}


fun LazyListScope.chapterListItems(
    chaptersProvider: () -> ImmutableList<Chapter>,
    downloadsProvider: () -> ImmutableList<Download>,
    showFullTitle: Boolean,
    onMarkAsRead: (id: String) -> Unit,
    onBookmark: (id: String) -> Unit,
    onDownloadClicked: (id: String) -> Unit,
    onCancelClicked: (download: Download) -> Unit,
    onPauseClicked: (download: Download) -> Unit,
    onDeleteClicked: (id: String) -> Unit,
    onReadClicked: (id: String) -> Unit,
) {
    items (
        items = chaptersProvider(),
        key = { c -> c.id }
    ){ chapter ->
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
                download = downloadsProvider().fastFirstOrNull { it.chapter.id == chapter.id },
                showFullTitle = showFullTitle,
                onDownloadClicked = { onDownloadClicked(chapter.id) },
                onDeleteClicked = {
                    onDeleteClicked(chapter.id)
                },
                onCancelClicked = onCancelClicked,
                onPauseClicked = onPauseClicked
            )
        }
    }
}

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
private fun ChapterListItem(
    modifier: Modifier = Modifier,
    showFullTitle: Boolean,
    chapter: Chapter,
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
        val status by remember(download) {
            download?.statusFlow
                ?: flowOf(
                    if (chapter.downloaded) Download.State.DOWNLOADED else Download.State.NOT_DOWNLOADED
                )
        }
            .collectAsState(Download.State.NOT_DOWNLOADED)

        val progress by remember(download) {
            download?.progressFlow ?: flowOf(0)
        }
            .collectAsState(0)

        ChapterDownloadIndicator(
            enabled = true,
            downloadStateProvider = { status },
            downloadProgressProvider = { progress },
            onClick = { action ->
                when (action) {
                    ChapterDownloadAction.START -> onDownloadClicked()
                    ChapterDownloadAction.START_NOW -> Unit
                    ChapterDownloadAction.CANCEL -> onCancelClicked(
                        download ?: return@ChapterDownloadIndicator
                    )
                    ChapterDownloadAction.DELETE -> onDeleteClicked()
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReaderOptions(
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val space = LocalSpacing.current

    var fullscreen by ReaderPrefs.fullscreen.collectAsState(true, scope)
    var showPageNumber by ReaderPrefs.showPageNumber.collectAsState(true, scope)
    var layout by ReaderPrefs.layoutDirection.collectAsState(
        defaultValue = ReaderLayout.PagedRTL,
        converter = Converters.LayoutDirectionConverter,
        scope
    )
    var backgroundColor by ReaderPrefs.backgroundColor.collectAsState(3, scope)

    Column(
        modifier.padding(space.med),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = "Reading mode")
            FlowRow(
                verticalArrangement = Arrangement.Center
            ) {
                FilterChip(
                    selected = layout == ReaderLayout.PagedLTR,
                    onClick = { layout = ReaderLayout.PagedLTR },
                    label = { Text("Paged (left to right)") },
                    modifier = Modifier.padding(space.small)
                )
                FilterChip(
                    selected = layout == ReaderLayout.PagedRTL,
                    onClick = { layout = ReaderLayout.PagedRTL },
                    label = { Text("Paged (right to left)") },
                    modifier = Modifier.padding(space.small)
                )
                FilterChip(
                    selected = layout == ReaderLayout.Vertical,
                    onClick = { layout = ReaderLayout.Vertical },
                    label = { Text("Vertical") },
                    modifier = Modifier.padding(space.small)
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = "Background color")
            FlowRow(
                verticalArrangement = Arrangement.Center
            ) {
                val colors = remember {
                    persistentListOf(
                        Color.Black to "Black",
                        Color.Gray to "Gray",
                        Color.White to "White",
                        Color.Unspecified to "Default"
                    )
                }
                colors.fastForEachIndexed { i, c ->
                    FilterChip(
                        selected = backgroundColor == i,
                        onClick = { backgroundColor = i },
                        label = { Text(c.second) },
                        modifier = Modifier.padding(space.small)
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = fullscreen, onCheckedChange = { fullscreen = it })
            Text("Fullscreen")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = showPageNumber, onCheckedChange = { showPageNumber = it })
            Text("Show page number")
        }
    }
}
