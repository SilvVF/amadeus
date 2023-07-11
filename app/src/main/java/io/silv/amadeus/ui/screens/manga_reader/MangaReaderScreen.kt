@file:OptIn(ExperimentalFoundationApi::class)

package io.silv.amadeus.ui.screens.manga_reader

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import io.silv.amadeus.ui.composables.AnimatedBoxShimmer
import io.silv.amadeus.ui.shared.CenterBox
import io.silv.amadeus.ui.theme.LocalPaddingValues
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.core.lerp
import io.silv.manga.domain.models.DomainChapter
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import kotlin.math.absoluteValue


class MangaReaderScreen(
    private val mangaId: String,
    private val chapterId: String
): Screen {

    @Composable
    override fun Content() {
        val sm = getScreenModel<MangaReaderSM>() { parametersOf(mangaId, chapterId) }

        val state by sm.mangaWithChapters.collectAsStateWithLifecycle()

        MangaReader(
            state = state,
            currentPage = { page ->
                sm.updateChapterPage(page)
            },
            goToChapter = {
                sm.goToChapter(it.id)
            },
            goToNextChapter = {
                sm.goToNextChapter(it)
            },
            goToPrevChapter = {
                sm.goToPrevChapter(it)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaReader(
    state: MangaReaderState,
    currentPage: (Int) -> Unit,
    goToNextChapter: (DomainChapter) -> Unit,
    goToPrevChapter: (DomainChapter) -> Unit,
    goToChapter: (DomainChapter) -> Unit
) {
    when (state) {
        MangaReaderState.Loading -> {
            CenterBox {
                AnimatedBoxShimmer(modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                )
            }
        }
        is MangaReaderState.Success -> {

            val scope = rememberCoroutineScope()
            val horizontalPagerState = rememberPagerState()
            val pageNumberPagerState = rememberPagerState()
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val paddingLocal by LocalPaddingValues.current
            val navigator = LocalNavigator.current
            val space = LocalSpacing.current

            fun resetPagerState() {
                scope.launch {
                    horizontalPagerState.scrollToPage(0)
                }
            }


            LaunchedEffect(horizontalPagerState) {
                suspend fun update(page: Int) {
                    pageNumberPagerState.animateScrollToPage(
                        page,
                        animationSpec = spring()
                    )
                    currentPage(page + 1)
                }
                launch {
                    snapshotFlow { horizontalPagerState.currentPage }.collect { page ->
                        update(page)
                    }
                }
            }

            BackHandler {
                if (drawerState.isOpen) {
                    scope.launch {
                        drawerState.close()
                    }
                } else {
                    scope.launch {
                        horizontalPagerState.animateScrollToPage(
                            page = horizontalPagerState.currentPage - 1
                        )
                    }
                }
            }

            ChapterInfoModalDrawer(
                drawerState = drawerState,
                modifier = Modifier.fillMaxWidth(0.75f),
                manga = state.manga,
                chapter = state.chapter,
                chapters = state.chapters,
                onChapterClicked = { chapter ->
                    resetPagerState()
                    goToChapter(chapter)
                },
                onGoToNextChapterClicked = {
                    resetPagerState()
                    goToNextChapter(it)
                },
                onGoToPrevChapterClicked = {
                    resetPagerState()
                    goToPrevChapter(it)
                }

            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(
                                    Modifier.fillMaxWidth().offset((-48).dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AnimatedPageNumber(
                                        modifier = Modifier.fillMaxWidth(0.5f),
                                        state = pageNumberPagerState,
                                        mangaPagerState = horizontalPagerState,
                                        pageCount = state.chapter.pages
                                    )
                                    if(horizontalPagerState.currentPage + 1 < state.chapter.pages - 1) {
                                        Text(
                                            text = "..${state.chapter.pages}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            },
                            navigationIcon = {
                                Row {
                                    IconButton(
                                        modifier = Modifier
                                            .padding(space.med)
                                            .size(32.dp),
                                        onClick = {
                                            navigator?.pop()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ArrowBack,
                                            contentDescription = null
                                        )
                                    }
                                    IconButton(
                                        modifier = Modifier
                                            .padding(space.med)
                                            .size(32.dp),
                                        onClick = {
                                            scope.launch {
                                                drawerState.open()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Menu,
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(paddingLocal)
                            .padding(paddingValues)
                    ) {
                        CenterBox(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            MangaImagePager(
                                imageUris = state.pages,
                                horizontalState = horizontalPagerState,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}





@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimatedPageNumber(
    modifier: Modifier = Modifier,
    state: PagerState,
    mangaPagerState: PagerState,
    pageCount: Int,
) {
        HorizontalPager(
            pageCount = pageCount,
            state = state,
            userScrollEnabled = false,
            pageSize = PageSize.Fixed(50.dp),
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 50.dp)
        ) { page ->
            CenterBox(
                Modifier.graphicsLayer {
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
                Text(text = (page + 1).toString())
            }
        }
}

