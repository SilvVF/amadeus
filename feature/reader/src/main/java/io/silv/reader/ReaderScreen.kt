@file:OptIn(ExperimentalFoundationApi::class)

package io.silv.reader

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.twotone.Archive
import androidx.compose.material.icons.twotone.Unarchive
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil.compose.AsyncImage
import coil.request.ImageRequest
import eu.kanade.tachiyomi.reader.model.ReaderChapter
import io.silv.common.model.Download
import io.silv.common.model.Page
import io.silv.datastore.asState
import io.silv.datastore.model.ReaderPrefs
import io.silv.model.SavableChapter
import io.silv.reader.composables.MenuPageSlider
import io.silv.ui.CenterBox
import io.silv.ui.composables.ChapterDownloadAction
import io.silv.ui.composables.ChapterDownloadIndicator
import io.silv.ui.layout.ExpandableInfoLayout
import io.silv.ui.layout.rememberExpandableState
import io.silv.ui.theme.LocalSpacing
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import org.koin.core.parameter.parametersOf
import java.nio.ByteBuffer
import kotlin.math.roundToInt

class ReaderScreen(
    val mangaId: String,
    val chapterId: String,
) : Screen {

    @Composable
    override fun Content() {

        val screenModel = getScreenModel<ReaderScreenModel> { parametersOf(mangaId, chapterId) }

        val state = screenModel.state.collectAsStateWithLifecycle().value

        LaunchedEffect(Unit) {
            screenModel.state.collectLatest {
                it.viewerChapters?.currChapter?.pages?.map { page ->
                    page.statusFlow.onEach {
                        Log.d(page.chapter.chapter.id, it.toString())
                    }
                        .launchIn(this)
                }
            }
        }

        Box(Modifier.fillMaxSize()) {
            if (state.viewerChapters != null) {
                HorizontalReader(
                    viewerChapters = state.viewerChapters,
                    onPageChange = screenModel::pageChanged,
                    chapterList = state.chapters,
                    loadPrevChapter = screenModel::loadPreviousChapter,
                    loadNextChapter = screenModel::loadNextChapter
                )
            } else {
                CenterBox(Modifier.fillMaxSize()) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HorizontalReader(
    viewerChapters: ViewerChapters,
    chapterList: ImmutableList<SavableChapter>,
    onPageChange: (readerChapter: ReaderChapter, page: Page) -> Unit,
    loadNextChapter: () -> Unit,
    loadPrevChapter: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.currentOrThrow

    val readerChapter = viewerChapters.currChapter

    val pagerState =
        rememberPagerState(
            initialPage = readerChapter.requestedPage,
        ) {
            readerChapter.pages?.size ?: 0
        }

    LaunchedEffect(readerChapter.chapter) {
        pagerState.scrollToPage(readerChapter.requestedPage)
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect {
            onPageChange(readerChapter, readerChapter.pages?.getOrNull(it) ?: return@collect)
        }
    }

    val layoutDirection by ReaderPrefs.layoutDirection.asState(
        defaultValue = LayoutDirection.Rtl,
        store = { dir -> dir.ordinal },
        convert = { v -> if (v == 1) LayoutDirection.Rtl else LayoutDirection.Ltr }
    )

    fun scrollToNextPage() {
        scope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage + 1)
        }
    }

    fun scrollToPage(page: Int) {
        scope.launch {
            pagerState.animateScrollToPage(page)
        }
    }

    fun scrollToPrevPage() {
        scope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage - 1)
        }
    }

    var menuVisible by rememberSaveable { mutableStateOf(false) }

    ReaderMenuOverlay(
        readerChapter = readerChapter,
        chapters = chapterList,
        menuVisible = menuVisible,
        currentPage = pagerState.currentPage,
        onDismissRequested = { menuVisible = false },
        changePage = ::scrollToPage,
        loadNextChapter = loadNextChapter,
        loadPrevChapter = loadPrevChapter,
        layoutDirection = layoutDirection,
        onBackArrowClick = {
            navigator.pop()
        },
        onViewOnWebClick = {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://mangadex.org/chapter/${readerChapter.chapter.id}")
                )
            )
        }
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides layoutDirection
        ) {
            GestureHintOverlay {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .zoneClickable {
                            when (layoutDirection) {
                                LayoutDirection.Ltr -> when (it) {
                                    Zone.LEFT -> scrollToPrevPage()
                                    Zone.MIDDLE -> menuVisible = !menuVisible
                                    Zone.RIGHT -> scrollToNextPage()
                                }

                                LayoutDirection.Rtl -> when (it) {
                                    Zone.LEFT -> scrollToNextPage()
                                    Zone.MIDDLE -> menuVisible = !menuVisible
                                    Zone.RIGHT -> scrollToPrevPage()
                                }
                            }
                        }
                ) { i ->

                    val page = readerChapter.pages?.getOrNull(i)
                        ?: return@HorizontalPager

                    val status by page.statusFlow.collectAsState()

                    LaunchedEffect(page) {
                        readerChapter.pageLoader?.loadPage(page)
                    }

                    when (status) {
                        Page.State.READY -> {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(
                                        page.stream?.let {
                                            ByteBuffer.wrap(it().readBytes())
                                        }
                                            ?: page.imageUrl
                                    )
                                    .build(),
                                modifier = Modifier.fillMaxSize(),
                                contentDescription = null
                            )
                        }
                        Page.State.QUEUE -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "Queued")
                                CircularProgressIndicator()
                            }
                        }
                        Page.State.LOAD_PAGE, Page.State.DOWNLOAD_IMAGE -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                CircularProgressIndicator(
                                    progress = page.progress.toFloat(),
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                        Page.State.ERROR -> {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "error"
                                )
                                Text("A problem occurred while loading the page")
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderMenuOverlay(
    readerChapter: ReaderChapter,
    chapters: ImmutableList<SavableChapter>,
    menuVisible: Boolean,
    currentPage: Int,
    layoutDirection: LayoutDirection,
    onDismissRequested: () -> Unit,
    loadPrevChapter: () -> Unit,
    loadNextChapter: () -> Unit,
    changePage: (page: Int) -> Unit,
    onBackArrowClick: () -> Unit,
    onViewOnWebClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val space = LocalSpacing.current
    val expandableState = rememberExpandableState(startProgress = SheetValue.PartiallyExpanded)

    LaunchedEffect(Unit) {
        snapshotFlow { expandableState.fraction }.collect {
            if (it == 1f) {
                onDismissRequested()
            }
        }
    }

    LaunchedEffect(menuVisible) {
       when {
           !menuVisible -> expandableState.hide()
           menuVisible && expandableState.isHidden -> expandableState.show()
       }
    }

    Box(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        content()
        AnimatedVisibility(
            visible = menuVisible,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it }
        ) {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackArrowClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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
                title = { Text(readerChapter.chapter.title) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            )
        }

        ExpandableInfoLayout(
            modifier = Modifier.align(Alignment.BottomCenter),
            state = expandableState,
            peekContent = {
                CompositionLocalProvider(
                    LocalLayoutDirection provides layoutDirection
                ) {
                    MenuPageSlider(
                        modifier = Modifier.padding(space.large),
                        fraction = expandableState.fraction,
                        page = currentPage,
                        lastPage = readerChapter.pages?.size ?: 0,
                        onPrevClick = loadPrevChapter,
                        onNextClick = loadNextChapter,
                        onPageChange = changePage
                    )
                }
            }
        ){
            Surface(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
            ) {
                LazyColumn(
                    Modifier
                        .fillMaxHeight(0.6f)
                        .fillMaxWidth()
                ) {
                    chapterListItems(
                        chapters,
                        onReadClicked = {},
                        onDeleteClicked = {},
                        downloads = persistentListOf(),
                        showFullTitle = true,
                        onMarkAsRead = {},
                        onBookmark = {},
                        onDownloadClicked = {},
                        onCancelClicked = {},
                        onPauseClicked = {}
                    )
                }
            }
        }
    }
}

@Composable
fun GestureHintOverlay(
    modifier: Modifier = Modifier,
    duration: Long = 2000,
    content: @Composable () -> Unit
) {

    val layoutDirection = LocalLayoutDirection.current

    var shownMenuHint by rememberSaveable { mutableStateOf<LayoutDirection?>(null) }
    var menuHintVisible by remember { mutableStateOf(false) }

    LaunchedEffect(layoutDirection) {
        if (shownMenuHint != layoutDirection) {
            shownMenuHint = layoutDirection
            menuHintVisible = true
            delay(duration)
            menuHintVisible = false
        }
    }


    Box(modifier) {

        content()

        if (menuHintVisible) {
            MenuHint { menuHintVisible = false }
        }
    }
}

@Composable
private fun MenuHint(
    hide: () -> Unit
) {
    Row(
        Modifier
            .fillMaxSize()
            .clickable { hide() }) {
        val ld = LocalLayoutDirection.current
        CenterBox(modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .background(Color.Green.copy(alpha = 0.38f))) {
            Text(
                when(ld) {
                    LayoutDirection.Ltr -> "Next"
                    LayoutDirection.Rtl -> "Prev"
                }
            )
        }
        CenterBox(modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .background(Color.Red.copy(alpha = 0.38f))) {
            Text("Menu")
        }
        CenterBox(modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .background(Color.Blue.copy(alpha = 0.38f))) {
            Text(
                when(ld) {
                    LayoutDirection.Ltr -> "Prev"
                    LayoutDirection.Rtl -> "Next"
                }
            )
        }
    }
}


fun LazyListScope.chapterListItems(
    chapters: ImmutableList<SavableChapter>,
    downloads: ImmutableList<Download>,
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
        items = chapters,
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
                download = downloads.fastFirstOrNull { it.chapter.id == chapter.id },
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

private enum class Zone {
    LEFT, MIDDLE, RIGHT
}

private fun Modifier.zoneClickable(
    onClick: (Zone) -> Unit
) = pointerInput(Unit) {

    val zoneSplit = this.size.width / 3

    detectTapGestures {
        val zone = when(it.x.roundToInt()){
            in 0..zoneSplit -> Zone.LEFT
            in zoneSplit..(size.width - zoneSplit) -> Zone.MIDDLE
            else -> Zone.RIGHT
        }
        onClick(zone)
    }
}
