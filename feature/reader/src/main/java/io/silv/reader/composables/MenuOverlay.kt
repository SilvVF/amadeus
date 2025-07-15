package io.silv.reader.composables


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.twotone.Archive
import androidx.compose.material.icons.twotone.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.AlphaTile
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.ColorPickerController
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.drawColorIndicator
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import io.silv.common.model.Download
import io.silv.common.model.ReaderLayout
import io.silv.data.chapter.Chapter
import io.silv.data.download.QItem
import io.silv.data.manga.model.Manga
import io.silv.reader.composables.ChapterList
import io.silv.reader.loader.ReaderChapter
import io.silv.reader2.ReaderSettings
import io.silv.reader2.ReaderSettingsEvent
import io.silv.reader2.ReaderSettingsEvent.ChangeLayout
import io.silv.ui.composables.ChapterDownloadAction
import io.silv.ui.composables.ChapterDownloadIndicator
import io.silv.ui.design.chapterListItems
import io.silv.ui.layout.DragAnchors
import io.silv.ui.layout.ExpandableInfoLayout
import io.silv.ui.layout.ExpandableScope
import io.silv.ui.layout.rememberExpandableState
import io.silv.ui.theme.LocalSpacing
import kotlinx.coroutines.launch
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import kotlin.math.roundToInt
import kotlin.toString

private enum class MenuTabs(val icon: ImageVector) {
    Chapters(Icons.Filled.FormatListNumbered),
    Options(Icons.Filled.Tune)
}

@Composable
private fun OverlayWithTabsLayout(
    modifier: Modifier = Modifier,
    menuVisible: () -> Boolean,
    onDismissRequested: () -> Unit,
    topBar: @Composable () -> Unit,
    menu: @Composable ExpandableScope.(MenuTabs) -> Unit,
    pageSelector: @Composable (fraction: Float) -> Unit,
    content: @Composable () -> Unit,
) {
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

    val space = LocalSpacing.current
    val menuPagerState = rememberPagerState { MenuTabs.entries.size }
    val scope = rememberCoroutineScope()

    Box(
        modifier.fillMaxSize()
    ) {
        content()
        AnimatedVisibility(
            visible = menuVisible(),
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it }
        ) {
            Box(Modifier.align(Alignment.TopCenter)) {
                topBar()
            }
        }
        ExpandableInfoLayout(
            modifier = Modifier.align(Alignment.BottomCenter),
            state = expandableState,
            peekContent = {
                Box {
                    Box(
                        Modifier
                            .padding(space.large)
                            .align(Alignment.Center)
                            .consumeWindowInsets(WindowInsets.systemBars)
                    ) {
                        pageSelector(expandableState.fraction.value)
                    }
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
                        MenuTabs.entries.fastForEach { menuTab ->
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
                    menu(MenuTabs.entries[it])
                }
            }
        }
    }
}

@Composable
fun ReaderMenuOverlay(
    modifier: Modifier = Modifier,
    settings: ReaderSettings,
    downloadsProvider: List<QItem<Download>>,
    readerChapter: () -> ReaderChapter?,
    manga: () -> Manga?,
    chapters: List<Chapter>,
    menuVisible: () -> Boolean,
    currentPage: () -> Int,
    pageCount: () -> Int,
    l2r: Boolean,
    onDismissRequested: () -> Unit,
    loadPrevChapter: () -> Unit,
    loadNextChapter: () -> Unit,
    changePage: (page: Int) -> Unit,
    onBackArrowClick: () -> Unit,
    onViewOnWebClick: () -> Unit,
    chapterActions: ChapterActions,
    content: @Composable () -> Unit,
) {
    OverlayWithTabsLayout(
        modifier = modifier,
        menuVisible = menuVisible,
        onDismissRequested = onDismissRequested,
        topBar = {
           ReaderTopBar(
               manga = manga,
               onBackArrowClick,
               onViewOnWebClick,
               readerChapter = readerChapter,
           )
        },
        pageSelector = { fraction ->
            MenuPageSlider(
                fractionProvider = { fraction },
                page = currentPage(),
                pageCountProvider = pageCount,
                onPrevClick = loadPrevChapter,
                onNextClick = loadNextChapter,
                onPageChange = changePage,
                l2r = l2r
            )
        },
        menu = { menu ->
            when (menu) {
                MenuTabs.Chapters -> ChapterList(
                    chapters = chapters,
                    downloadsProvider = downloadsProvider,
                    modifier = Modifier
                        .fillMaxHeight(0.6f)
                        .fillMaxWidth(),
                    actions = chapterActions
                )

                MenuTabs.Options -> ReaderOptions(
                    settings = settings,
                    modifier = Modifier
                        .fillMaxHeight(0.6f)
                        .fillMaxWidth()
                )
            }
        }
    ) {
        content()
    }
}


@Composable
private fun ReaderTopBar(
    manga: () -> Manga?,
    onBackArrowClick: () -> Unit,
    onViewOnWebClick: () -> Unit,
    readerChapter: () -> ReaderChapter?,
    modifier: Modifier = Modifier
) {
    LargeTopAppBar(
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        ),
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
                horizontalAlignment = Alignment.Start
            ) {
                val chapter = readerChapter()
                val chapterText = remember(chapter) {
                    chapter?.let {
                        if (chapter.chapter.validNumber) {
                            "Ch.${chapter.chapter.chapter} - ${chapter.chapter.title}"
                        } else {
                            chapter.chapter.title
                        }
                    } ?: ""
                }
                val contentColor = LocalContentColor.current
                BasicText(
                    style = LocalTextStyle.current,
                    text = manga()?.titleEnglish.orEmpty(),
                    autoSize = TextAutoSize.StepBased(maxFontSize = MaterialTheme.typography.titleLarge.fontSize),
                    maxLines = 1,
                    color = ColorProducer { contentColor }
                )
                Text(
                    chapterText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .78f)
                    ),
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}

@Stable
data class ChapterActions(
    val delete: (chapterId: String) -> Unit = {},
    val loadChapter: (chapterId: String) -> Unit = {},
    val markRead: (chapterId: String) -> Unit = {},
    val bookmark: (chapterId: String) -> Unit = {},
    val cancelDownload: (download: Download) -> Unit = {},
    val pauseDownloads: () -> Unit = {},
    val download: (chapterId: String) -> Unit = {}
)

@Composable
private fun ExpandableScope.ChapterList(
    modifier: Modifier = Modifier,
    downloadsProvider: List<QItem<Download>>,
    chapters: List<Chapter>,
    actions: ChapterActions,
) {
    val lazyListState = rememberLazyListState()
    LazyColumn(
        state = lazyListState,
        modifier = modifier.nestedScroll(lazyListState)
    ) {
        chapterListItems(
            chapters,
            onReadClicked = { actions.loadChapter(it) },
            onDeleteClicked = { actions.delete(it) },
            downloads = downloadsProvider,
            showFullTitle = true,
            onMarkAsRead = { actions.markRead(it) },
            onBookmark = { actions.bookmark(it) },
            onDownloadClicked = { actions.download(it) },
            cancelDownload = { actions.cancelDownload(it) },
            pauseDownload = { actions.pauseDownloads() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReaderOptions(
    settings: ReaderSettings,
    modifier: Modifier = Modifier
) {
    val space = LocalSpacing.current
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
                val sharedModifier = Modifier.padding(space.small)
                FilterChip(
                    selected = settings.layout == ReaderLayout.PagedLTR,
                    onClick = { settings.events(ChangeLayout(ReaderLayout.PagedLTR)) },
                    label = { Text("Paged (left to right)") },
                    modifier = sharedModifier
                )
                FilterChip(
                    selected = settings.layout == ReaderLayout.PagedRTL,
                    onClick = { settings.events(ChangeLayout(ReaderLayout.PagedRTL)) },
                    label = { Text("Paged (right to left)") },
                    modifier = sharedModifier
                )
                FilterChip(
                    selected = settings.layout == ReaderLayout.Vertical,
                    onClick = { settings.events(ChangeLayout(ReaderLayout.Vertical)) },
                    label = { Text("Vertical") },
                    modifier = sharedModifier
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
                    listOf(
                        Color.Black to "Black",
                        Color.Gray to "Gray",
                        Color.White to "White",
                    )
                }
                val sharedModifier = Modifier.padding(space.small)
                colors.fastForEach { (color, text) ->
                    FilterChip(
                        selected = settings.color == color,
                        onClick = { settings.events(ReaderSettingsEvent.ChangeColor(color)) },
                        label = { Text(text) },
                        modifier = sharedModifier
                    )
                }

                var pickingCustomColor by rememberSaveable {
                    mutableStateOf(false)
                }
                val isCustom by remember(settings.color) {
                    derivedStateOf { settings.color !in colors.map { it.first } }
                }

                if (pickingCustomColor) {
                    val controller = rememberColorPickerController()
                    Dialog(
                        onDismissRequest = { pickingCustomColor = false }
                    ) {
                        HsvColorPickerColoredSelectorScreen(
                            controller = controller,
                            onCancel = { pickingCustomColor = false },
                            initialColor = settings.color,
                            onConfirm = {
                                settings.events(ReaderSettingsEvent.ChangeColor(controller.selectedColor.value))
                                pickingCustomColor = false
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                FilterChip(
                    border = if (isCustom) {
                        BorderStroke(2.dp, settings.color)
                    } else {
                        FilterChipDefaults.filterChipBorder(true, false)
                    },
                    selected = isCustom,
                    label = {
                        Text("Custom ${if (isCustom) "(#${settings.color.hexCode})" else ""}")
                    },
                    modifier = sharedModifier,
                    onClick = {
                        pickingCustomColor = true
                    }
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = settings.fullscreen, onCheckedChange = {
                settings.events(ReaderSettingsEvent.ToggleFullscreen)
            })
            Text("Fullscreen")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = settings.showPageNumber, onCheckedChange = {
                settings.events(
                    ReaderSettingsEvent.ToggleShowPageNumber
                )
            })
            Text("Show page number")
        }
    }
}

val Color.hexCode: String
    inline get() {
        val a: Int = (alpha * 255).toInt()
        val r: Int = (red * 255).toInt()
        val g: Int = (green * 255).toInt()
        val b: Int = (blue * 255).toInt()
        return a.hex + r.hex + g.hex + b.hex
    }

val Int.hex get() = this.toString(16).padStart(2, '0')

@Composable
fun HsvColorPickerColoredSelectorScreen(
    onConfirm: (Color) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    initialColor: Color? = null,
    controller: ColorPickerController = rememberColorPickerController()
) {
    var hexCode by remember { mutableStateOf("") }
    var textColor by remember { mutableStateOf(Color.Transparent) }

    Column(modifier) {
        Spacer(modifier = Modifier.weight(1f))

        Row(modifier = Modifier.weight(3f)) {
            listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Gray)
                .forEach { color ->
                    Surface(
                        modifier = Modifier
                            .height(40.dp)
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        color = color,
                        shape = RoundedCornerShape(6.dp),
                        onClick = {
                            controller.selectByColor(color, true)
                        },
                    ) {}
                }
        }

        Box(modifier = Modifier.weight(8f)) {
            HsvColorPicker(
                modifier = Modifier
                    .padding(10.dp),
                initialColor = initialColor,
                controller = controller,
                drawOnPosSelected = {
                    drawColorIndicator(
                        controller.selectedPoint.value,
                        controller.selectedColor.value,
                    )
                },
                onColorChanged = { colorEnvelope ->
                    hexCode = colorEnvelope.hexCode
                    textColor = colorEnvelope.color
                },
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        AlphaSlider(
            modifier = Modifier
                .testTag("HSV_AlphaSlider")
                .fillMaxWidth()
                .padding(10.dp)
                .height(35.dp)
                .align(Alignment.CenterHorizontally),
            controller = controller,
        )

        BrightnessSlider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .height(35.dp)
                .align(Alignment.CenterHorizontally),
            controller = controller,
        )

        Text(
            text = "#$hexCode",
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        AlphaTile(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(6.dp))
                .align(Alignment.CenterHorizontally),
            controller = controller,
        )
        Spacer(modifier = Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                onClick = onCancel
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    onConfirm(controller.selectedColor.value)
                }
            ) {
                Text("Confirm")
            }
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}