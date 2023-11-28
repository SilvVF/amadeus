package io.silv.amadeus.ui.screens.manga_reader.composables

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.silv.amadeus.types.SavableChapter
import io.silv.amadeus.ui.composables.getSize
import io.silv.common.model.ReaderDirection
import io.silv.common.model.ReaderOrientation
import io.silv.datastore.model.ReaderSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderMenuOverlay(
    modifier: Modifier,
    readerSettings: ReaderSettings,
    onSettingsChanged: (ReaderSettings) -> Unit,
    mangaTitle: String,
    chapter: SavableChapter,
    chapters: List<SavableChapter>,
    lastPage: Int,
    currentPage: Int,
    onPageChange: (page: Int) -> Unit,
    handleBackGesture: (orientation: ReaderOrientation) -> Unit,
    handleForwardGesture: (orientation: ReaderOrientation) -> Unit,
    onNavigationIconClick: () -> Unit,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onChapterBookmarked: (id: String) -> Unit,
    goToChapter: (id: String) -> Unit,
    content: @Composable () -> Unit
) {
    var visible by rememberSaveable {
        mutableStateOf(false)
    }
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            confirmValueChange = { it != SheetValue.Hidden }
        )
    )

    var maxX by remember { mutableStateOf(0.dp) }

    Box(
        modifier = modifier
            .getSize { size -> maxX = size.width }
            .pointerInput(Unit) {
                detectTapGestures {
                    val third = maxX.div(3f)
                    when (it.x.toDp()) {
                        in 0.dp..third ->  {
                            Log.d("Reader", "Back gesture $readerSettings")
                            when (readerSettings.orientation) {
                                ReaderOrientation.Vertical -> handleBackGesture(readerSettings.orientation)
                                ReaderOrientation.Horizontal ->  when(readerSettings.direction) {
                                    ReaderDirection.Ltr -> handleBackGesture(readerSettings.orientation)
                                    ReaderDirection.Rtl -> handleForwardGesture(readerSettings.orientation)
                                }
                            }
                        }
                        in (third * 2)..maxX -> {
                            Log.d("Reader", "Forward gesture $readerSettings")
                            when (readerSettings.orientation) {
                                ReaderOrientation.Vertical -> handleForwardGesture(readerSettings.orientation)
                                ReaderOrientation.Horizontal ->  when(readerSettings.direction) {
                                    ReaderDirection.Ltr -> handleForwardGesture(readerSettings.orientation)
                                    ReaderDirection.Rtl -> handleBackGesture(readerSettings.orientation)
                                }
                            }
                        }
                        else -> {
                            visible = !visible
                        }
                    }
                }
            }
    ) {
        content()
        if (visible) {
            ScaffoldOverlay(
                scaffoldState = scaffoldState,
                readerSettings = readerSettings,
                onSettingsChanged = onSettingsChanged,
                topBar = {
                    MangaChapterInfoTopBar(
                        mangaTitle = mangaTitle,
                        chapterTitle = chapter.title,
                        onNavigationIconClick = onNavigationIconClick
                    )
                },
                viewing = chapter,
                lastPage = lastPage,
                page = currentPage,
                chapterList = chapters,
                onPrevClick = onPrevClick,
                onNextClick = onNextClick,
                onPageChange = onPageChange,
                onChapterBookmarked = onChapterBookmarked,
                goToChapter = goToChapter
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaChapterInfoTopBar(
    mangaTitle: String,
    chapterTitle: String,
    onNavigationIconClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.DarkGray.copy(alpha = 0.5f),
            scrolledContainerColor = Color.DarkGray.copy(alpha = 0.5f),
        ),
        title = {
            Column(
                Modifier.fillMaxWidth(0.8f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(mangaTitle, 
                    textAlign = TextAlign.Center,
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis
                )
                Text(chapterTitle, 
                    color = Color.LightGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
}



@Composable
fun MenuSelections(
    modifier: Modifier,
    onListIconClick: () -> Unit,
    onTuneIconClick: () -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onListIconClick
        ) {
            Icon(
                imageVector = Icons.Filled.FormatListNumbered,
                contentDescription = null
            )
        }
        IconButton(
            onClick = onTuneIconClick
        ) {
            Icon(
                imageVector = Icons.Filled.Tune,
                contentDescription = null
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ScaffoldOverlay(
    scaffoldState: BottomSheetScaffoldState,
    readerSettings: ReaderSettings,
    onSettingsChanged: (ReaderSettings) -> Unit,
    topBar: @Composable () -> Unit,
    viewing: SavableChapter,
    lastPage: Int,
    page: Int,
    chapterList: List<SavableChapter>,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onPageChange: (page: Int) -> Unit,
    onChapterBookmarked: (id: String) -> Unit,
    goToChapter: (id: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val backgroundColor by animateColorAsState(
        targetValue = if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
            MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
        } else {
            Color.DarkGray.copy(alpha = 0.9f)
        },
        label = "background-color-for-sheet"
    )

    fun updateBottomSheetState() {
        scope.launch {
            when (scaffoldState.bottomSheetState.currentValue) {
                SheetValue.Hidden -> scaffoldState.bottomSheetState.show()
                SheetValue.Expanded -> scaffoldState.bottomSheetState.partialExpand()
                SheetValue.PartiallyExpanded -> scaffoldState.bottomSheetState.expand()
            }
        }
    }

    BottomSheetScaffold(
        topBar = topBar,
        sheetContainerColor = Color.Transparent,
        scaffoldState = scaffoldState,
        containerColor = Color.Transparent,
        sheetPeekHeight = 172.dp,
        sheetTonalElevation = 0.dp,
        sheetDragHandle = {},
        sheetContent = {
            Column {
               MenuPageSlider(
                   visible = scaffoldState.bottomSheetState.currentValue == SheetValue.PartiallyExpanded,
                   page = page,
                   lastPage = lastPage,
                   onPrevClick = onPrevClick,
                   onNextClick = onNextClick,
                   onPageChange = onPageChange
               )
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
                    var showingList by rememberSaveable {
                        mutableStateOf(true)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    MenuSelections(
                        modifier = Modifier.fillMaxWidth(),
                        onListIconClick = {
                            if (showingList) {
                                updateBottomSheetState()
                            } else {
                                showingList = true
                            }
                        },
                        onTuneIconClick = {
                            if (!showingList) {
                                updateBottomSheetState()
                            } else {
                                showingList = false
                            }
                        }
                    )
                    AnimatedContent(targetState = showingList, label = "menu_selections") {
                        if (showingList) {
                            ChaptersList(
                                modifier = Modifier.fillMaxSize(),
                                chapters = chapterList,
                                selected = viewing,
                                onBookmarkClick = onChapterBookmarked,
                                onChapterClicked = goToChapter
                            )
                        } else { 
                            ReaderSettingsMenu(
                                settings = readerSettings,
                                onSettingsChanged = onSettingsChanged
                            )
                        }
                    }
                }
            }
        }
    ){}
}


