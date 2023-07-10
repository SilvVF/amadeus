@file:OptIn(ExperimentalFoundationApi::class)

package io.silv.amadeus.ui.screens.manga_reader

import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import io.silv.amadeus.ui.composables.AnimatedBoxShimmer
import io.silv.amadeus.ui.shared.CenterBox
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
                sm.goToNextChapter()
            },
            goToPrevChapter = {
                sm.goToPrevChapter()
            }
        )
    }
}

@Composable
fun MangaReader(
    state: MangaReaderState,
    currentPage: (Int) -> Unit,
    goToNextChapter: () -> Unit,
    goToPrevChapter: () -> Unit,
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

            val pagerState = rememberPagerState(initialPage = 0)
            val pageNumberPagerState = rememberPagerState()

            LaunchedEffect(key1 = pagerState) {
                launch {
                    snapshotFlow { pagerState.currentPage }.collect { page ->
                        pageNumberPagerState.animateScrollToPage(
                            page,
                            animationSpec = spring()
                        )
                        currentPage(page)
                    }
                }
            }
            ChapterInfoModalDrawer(
                modifier = Modifier.fillMaxWidth(0.75f),
                manga = state.manga,
                chapter = state.chapter,
                chapters = state.chapters,
                onChapterClicked = { chapter ->
                    goToChapter(chapter)
                },
                onGoToNextChapterClicked = goToNextChapter,
                onGoToPrevChapterClicked = goToPrevChapter

            ) {
                Scaffold { paddingValues ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        CenterBox(Modifier
                            .fillMaxWidth()
                            .height(600.dp)
                        ) {
                            MangaImagePager(
                                imageUris = state.pages,
                                state = pagerState
                            )
                        }
                    }
                }
            }
        }
    }
}





@Composable
fun AnimatedPageNumber(
    state: PagerState,
    mangaPagerState: PagerState,
    pageCount: Int
) {
    Row(
        Modifier
            .fillMaxWidth()
            .systemBarsPadding(),
        horizontalArrangement = Arrangement.Center
    ) {
        HorizontalPager(
            pageCount = pageCount,
            state = state,
            userScrollEnabled = false,
            pageSize = PageSize.Fixed(50.dp),
            modifier = Modifier.fillMaxWidth(0.4f),
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
}

