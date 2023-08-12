@file:OptIn(ExperimentalFoundationApi::class)

package io.silv.amadeus.ui.screens.manga_reader

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.silv.amadeus.LocalBottomBarVisibility
import io.silv.amadeus.ui.composables.AnimatedBoxShimmer
import io.silv.amadeus.ui.composables.getSize
import io.silv.amadeus.ui.shared.CenterBox
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.core.lerp
import io.silv.manga.domain.models.SavableChapter
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import kotlin.math.absoluteValue
import kotlin.math.roundToInt


class MangaReaderScreen(
    private val mangaId: String,
    private val chapterId: String,
): Screen {

    @Composable
    override fun Content() {

        val sm = getScreenModel<MangaReaderSM>() { parametersOf(mangaId, chapterId) }

        val state by sm.mangaReaderState.collectAsStateWithLifecycle()

        var bottomBarVisibility by LocalBottomBarVisibility.current

        DisposableEffect(Unit) {
            bottomBarVisibility = false
            onDispose {
                bottomBarVisibility = true
            }
        }

        MangaReaderContent(state = state)
    }
}

@Composable
fun MangaReaderContent(
    state: MangaReaderState
) {
    when (state) {
        is MangaReaderState.Failure -> {
            CenterBox(Modifier.fillMaxSize()) {
                Text(state.message ?: "failed to load")
            }
        }
        MangaReaderState.Loading -> {
            CenterBox(Modifier.fillMaxSize()) {
                AnimatedBoxShimmer(Modifier.fillMaxSize(0.8f))
            }
        }
        is MangaReaderState.Success -> {
            val scope = rememberCoroutineScope()
            val horizontalReaderState = rememberPagerState()
            MangaMenuBox(
                modifier = Modifier.fillMaxSize(),
                onPrevPageGesture = {
                    scope.launch {
                        horizontalReaderState.animateScrollToPage(horizontalReaderState.currentPage - 1)
                    }
                },
                onNextPageGesture = {
                    scope.launch {
                        horizontalReaderState.animateScrollToPage(horizontalReaderState.currentPage + 1)
                    }
                },
                onNavigationIconClick = {},
                chapter = state.chapter,
                chapters = state.chapters,
                onPageChange = {
                    scope.launch {
                        horizontalReaderState.animateScrollToPage(it)
                    }
                },
                currentPage = horizontalReaderState.currentPage,
                mangaTitle = state.manga.titleEnglish
            ) {
                MangaReader(
                    viewing = state.chapter,
                    images = state.pages,
                    prev = remember(state.chapters, state.chapter) {
                        val idx = state.chapters.map { it.id }.also { Log.d("CHAPTERS", it.toString()) }.indexOf(state.chapter.id)
                        state.chapters.getOrNull(idx - 1)
                    },
                    next = remember(state.chapters, state.chapter) {
                        val idx = state.chapters.map { it.id }.indexOf(state.chapter.id)
                        state.chapters.getOrNull(idx + 1)
                    },
                    settings = remember { ReaderSettings() },
                    horizontalReaderState = horizontalReaderState
                )
            }
        }
    }
}

data class ReaderSettings(
    val orientation: Orientation = Orientation.Horizontal,
    val direction: LayoutDirection = LayoutDirection.Ltr,
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaMenuBox(
    modifier: Modifier,
    mangaTitle: String,
    chapter: SavableChapter,
    chapters: List<SavableChapter>,
    currentPage: Int,
    onPageChange: (page: Int) -> Unit,
    onNextPageGesture: () -> Unit,
    onPrevPageGesture: () -> Unit,
    onNavigationIconClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val space = LocalSpacing.current
    var visible by rememberSaveable {
        mutableStateOf(false)
    }
    val scope = rememberCoroutineScope()


    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            confirmValueChange = { it != SheetValue.Hidden }
        )
    )

    var maxX by remember { mutableStateOf(0.dp) }

    Box(
        modifier = modifier
            .getSize { size ->
                maxX = size.width
            }
            .pointerInput(Unit) {
                detectTapGestures {
                    Log.d("MenuBox", "xdp: ${it.x.toDp()} ydp: ${it.y.toDp()}")
                    val third = maxX.div(3f)
                    when (it.x.toDp()) {
                        in 0.dp..third -> {
                            Log.d("MenuBox", "in 1 / 3")
                            onPrevPageGesture()
                        }

                        in third..(third * 2) -> {
                            Log.d("MenuBox", "in 2 / 3")
                            visible = !visible
                        }

                        in (third * 2)..maxX -> {
                            Log.d("MenuBox", "in 3 / 3")
                            onNextPageGesture()
                        }
                    }
                }
            }
    ) {
        content()
        if (visible) {
            BottomSheetScaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.DarkGray.copy(alpha = 0.7f),
                            scrolledContainerColor = Color.DarkGray.copy(alpha = 0.7f),
                        ),
                        title = {
                            Column(Modifier.fillMaxWidth(0.8f),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(mangaTitle, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(chapter.title, color = Color.LightGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigationIconClick) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = null
                                )
                            }
                        }
                      )
                },
                sheetContainerColor = Color.Transparent,
                scaffoldState = scaffoldState,
                containerColor = Color.Transparent,
                sheetPeekHeight = 172.dp,
                sheetTonalElevation = 0.dp,
                sheetDragHandle = {},
                sheetContent = {

                    val backgroundColor by animateColorAsState(
                        targetValue = if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
                            Color.Black.copy(alpha = 0.9f)
                        } else {
                            Color.DarkGray.copy(alpha = 0.9f)
                        },
                        label = "background-color-for-sheet"
                    )

                    Column {
                        AnimatedVisibility(visible = scaffoldState.bottomSheetState.currentValue != SheetValue.Expanded) {
                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .height(70.dp)
                                .clip(RoundedCornerShape(100))
                                .background(
                                    Color.DarkGray.copy(alpha = 0.9f)
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = {},
                                    modifier = Modifier.padding(space.small)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SkipPrevious,
                                        contentDescription = null
                                    )
                                }
                                Text(
                                    (currentPage + 1).toString(),
                                    modifier = Modifier.padding(space.small)
                                )
                                Slider(
                                    valueRange = 0f..chapter.pages.toFloat(),
                                    value = currentPage.toFloat(),
                                    onValueChange = {
                                        onPageChange(it.roundToInt())
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(space.small),
                                    steps = chapter.pages
                                )
                                Text(chapter.pages.toString(), modifier = Modifier.padding(space.small))
                                IconButton(
                                    onClick = {},
                                    modifier = Modifier.padding(space.small)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SkipNext,
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxHeight(0.4f)
                                .fillMaxWidth()
                                .clip(
                                    RoundedCornerShape(12.dp)
                                )
                                .background(backgroundColor)
                        ) {
                            Row(Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            when (scaffoldState.bottomSheetState.currentValue) {
                                                SheetValue.Hidden -> scaffoldState.bottomSheetState.show()
                                                SheetValue.Expanded -> scaffoldState.bottomSheetState.partialExpand()
                                                SheetValue.PartiallyExpanded -> scaffoldState.bottomSheetState.expand()
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FormatListNumbered,
                                        contentDescription = null
                                    )
                                }
                                IconButton(
                                    onClick = {}
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Tune,
                                        contentDescription = null
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyColumn {
                                chapters.fastForEach {
                                    item(it) {
                                        Column {
                                            Row(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(space.med),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text(text = "Vol.${it.volume} Ch.${it.chapter} - ${it.title}")
                                                    Text(text = "${it.createdAt.month.name} ${it.createdAt.dayOfMonth}, ${it.createdAt.year} * ${it.scanlationGroupToId?.first}")
                                                }
                                                IconButton(onClick = { }) {
                                                    Icon(
                                                        imageVector = Icons.Filled.BookmarkBorder,
                                                        contentDescription = null
                                                    )
                                                }
                                            }
                                            Divider()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            ){}
        }
    }
}

@Composable
fun MangaReader(
    horizontalReaderState: PagerState,
    viewing: SavableChapter,
    images: List<String>,
    prev: SavableChapter?,
    next: SavableChapter?,
    settings: ReaderSettings
) {
    val space = LocalSpacing.current
    when (settings.orientation) {
        Orientation.Vertical -> {
            
        }
        Orientation.Horizontal -> {

            Column(Modifier.fillMaxSize()) {
                HorizontalReader(
                    modifier = Modifier.weight(1f),
                    viewing = viewing,
                    images = images,
                    prev = prev,
                    next = next,
                    pagerState = horizontalReaderState,
                    reverseLayout = when(settings.direction) {
                        LayoutDirection.Ltr -> false
                        LayoutDirection.Rtl -> true
                    }
                )
                Spacer(modifier = Modifier.height(space.med))
                AnimatedPageNumber(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    mangaPagerState = horizontalReaderState,
                    pageCount = images.size
                )
                Spacer(modifier = Modifier.height(space.med))
            }
        }
    }
}

@Composable
fun VerticalReader(
    viewing: SavableChapter,
    images: List<String>,
    prev: SavableChapter?,
    next: SavableChapter?,
) {
    
    
}

@Composable
fun HorizontalReader(
    modifier: Modifier = Modifier,
    viewing: SavableChapter,
    pagerState: PagerState,
    images: List<String>,
    prev: SavableChapter?,
    next: SavableChapter?,
    reverseLayout: Boolean,
) {
    val pageCount =  images.size + if (prev != null) 1 else 0 + if (next != null) 1 else 0
    HorizontalPager(
        modifier = modifier,
        pageCount = pageCount,
        state = pagerState,
        pageSize = PageSize.Fill,
        reverseLayout = reverseLayout
    ) {
        if (it == 0) {
            prev?.let { 
                CenterBox(Modifier.fillMaxSize()) {
                    Text("${it.title}  ch ${it.chapter}")
                }
                return@HorizontalPager
            }
        } else if (it >= images.size) {
            next?.let {
                CenterBox(Modifier.fillMaxSize()) {
                    Text("${it.title}  ch ${it.chapter}")
                }
                return@HorizontalPager
            }
        }
        MangaImage(
            modifier = Modifier.fillMaxSize(),
            url = images.getOrElse(it - if (prev != null) 1 else 0) { "" }
        )
    }
}

@Composable
fun MangaImage(
    modifier: Modifier,
    url: String
) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .build(),
        modifier= modifier,
        contentScale = ContentScale.Fit,
        contentDescription = null
    )
}




@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimatedPageNumber(
    modifier: Modifier = Modifier,
    state: PagerState = rememberPagerState(),
    mangaPagerState: PagerState,
    pageCount: Int,
) {

    LaunchedEffect(Unit) {
        snapshotFlow { mangaPagerState.currentPage }.collect {
            state.animateScrollToPage(it)
        }
    }

    var lastPage by remember {
        mutableStateOf(0)
    }
    var offset by remember {
        mutableStateOf(0.dp)
    }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val density = LocalDensity.current

    HorizontalPager(
        pageCount = pageCount,
        state = state,
        userScrollEnabled = false,
        pageSize = PageSize.Fixed(50.dp),
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = (screenWidth / 2f) - offset
        )
    ) { page ->
        LaunchedEffect(Unit) {
            lastPage = maxOf(page, lastPage)
        }

        CenterBox(
            Modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    offset = with(density) { (it.size.width / 2).toDp() }
                }
                .graphicsLayer {
                    val pageOffset = ((mangaPagerState.currentPage - page) + mangaPagerState
                        .currentPageOffsetFraction
                            ).absoluteValue
                    val interpolation = lerp(
                        start = 0.5f,
                        stop = 1f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    )
                    scaleX = interpolation
                    scaleY = interpolation
                    alpha = interpolation
                }
        ) {
            Text(
                text = (page + 1).toString(),
                textAlign = TextAlign.Center
            )
        }
    }
}

