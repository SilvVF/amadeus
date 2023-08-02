package io.silv.amadeus.ui.screens.manga_view

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import com.skydoves.orbital.Orbital
import com.skydoves.orbital.animateMovement
import com.skydoves.orbital.rememberContentWithOrbitalScope
import io.silv.amadeus.ui.composables.AnimatedShimmer
import io.silv.amadeus.ui.composables.MangaViewPoster
import io.silv.amadeus.ui.screens.manga_reader.MangaReaderScreen
import io.silv.amadeus.ui.screens.manga_view.composables.ChapterList
import io.silv.amadeus.ui.screens.manga_view.composables.VolumeList
import io.silv.amadeus.ui.shared.CenterBox
import io.silv.amadeus.ui.shared.noRippleClickable
import io.silv.amadeus.ui.stateholders.rememberSortedChapters
import io.silv.amadeus.ui.theme.LocalBottomBarVisibility
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.domain.models.DomainChapter
import io.silv.manga.domain.models.DomainManga
import org.koin.core.parameter.parametersOf


class MangaViewScreen(
  private val manga: DomainManga
): Screen {

    @Composable
    override fun Content() {

        val sm = getScreenModel<MangaViewSM> { parametersOf(manga) }

        var bottomBarVisible by LocalBottomBarVisibility.current

        val state by sm.chapterInfoUiState.collectAsStateWithLifecycle()
        val downloading by sm.downloadingOrDeleting.collectAsStateWithLifecycle()
        val navigator = LocalNavigator.current

        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(Unit) { bottomBarVisible = false }

        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { paddingValues ->
            Column(Modifier
                .padding(paddingValues)
            ) {
               MangaViewPoster(
                   Modifier
                       .fillMaxHeight(0.4f)
                       .fillMaxWidth(),
                   manga = state.manga,
                   onReadNowClick = {

                   },
                   onBookMarkClick = {
                       sm.bookmarkManga(state.manga.id)
                   }
               )
               MangaView(
                   state = state.chapterListState,
                   coverArtState = state.coverArtState,
                   downloads = downloading,
                   retryLoadCoverArt = {

                   },
                   retryLoadChapterList = {

                   },
                   downloadChapters = { chapters ->
                        sm.downloadChapterImages(
                            chapters.map { it.id }
                        )
                   },
                   deleteChapters = {chapters ->
                        sm.deleteChapterImages(
                            chapters.map { it.id }
                        )
                   },
                   readButtonClick = { mangaId, chapterId ->
                       navigator?.push(
                           MangaReaderScreen(mangaId, chapterId)
                       )
                   }
               )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MangaView(
    state: ChapterListState,
    coverArtState: CoverArtState,
    downloads: List<String>,
    retryLoadCoverArt: () -> Unit,
    retryLoadChapterList: () -> Unit,
    readButtonClick: (String, String) -> Unit,
    downloadChapters: (List<DomainChapter>) -> Unit,
    deleteChapters: (List<DomainChapter>) -> Unit,
) {

    val space = LocalSpacing.current

    when(state) {
        is ChapterListState.Failure -> {
            CenterBox(Modifier.fillMaxSize()) {
                Text(state.message)
                Button(onClick = retryLoadChapterList) {
                    Text(text = "Try again")
                }
            }
        }
        ChapterListState.Loading -> {
            Column(Modifier.fillMaxSize()) {
                repeat(5) {
                    AnimatedShimmer()
                }
            }
        }
        is ChapterListState.Success -> {

            val sortedChapters = rememberSortedChapters(chapters = state.chapters)
            var showChapters by rememberSaveable {
                mutableStateOf(true)
            }

            Column(Modifier.fillMaxSize()) {
                MangaViewFilterRow(
                    modifier = Modifier
                        .padding(vertical = space.med)
                        .fillMaxWidth()
                        .height(40.dp),
                    selectedTab = showChapters,
                    onTabSelected = {
                        showChapters = !showChapters
                    }
                )
                AnimatedContent(targetState = showChapters, label = "ikdjf") {
                    when (it) {
                        true -> {
                            ChapterList(
                                sortedChapters.sortedChapters,
                                sortedChapters.sortBy,
                                downloads,
                                sortByChange = { sortedChapters.sortByOpposite() },
                                downloadChapterClicked = {
                                    downloadChapters(
                                        listOf(it)
                                    )
                                },
                                deleteChapterClicked = {
                                    deleteChapters(listOf(it))
                                },
                                readButtonClick = {
                                    readButtonClick(
                                        it.mangaId,
                                        it.id
                                    )
                                }
                            )
                        }
                        false -> {
                            VolumeList(
                                volumeItems = sortedChapters.sortedVolumes,
                                coverArtState = coverArtState,
                                onSortByChange = { sortedChapters.sortByOpposite() },
                                onRetryLoadCoverArt =  retryLoadCoverArt,
                                sortBy = sortedChapters.sortBy
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MangaViewFilterRow(
    modifier: Modifier = Modifier,
    selectedTab: Boolean,
    onTabSelected: () -> Unit
) {
    val movementSpec = SpringSpec<IntOffset>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = 200f
    )

    val backgroundBox = rememberContentWithOrbitalScope {
            Box(
                Modifier
                    .fillMaxWidth(0.5f)
                    .animateMovement(
                        this@rememberContentWithOrbitalScope,
                        movementSpec
                    )
            ) {
               Box(modifier = Modifier
                   .fillMaxSize()
                   .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
               )
            }

    }

    Column(modifier) {
        Orbital(
            Modifier
                .noRippleClickable { onTabSelected() }
                .fillMaxWidth()
        ) {
            if (selectedTab) {
                Box(Modifier
                    .fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    backgroundBox()
                    FilterRowTextItems()
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    backgroundBox()
                    FilterRowTextItems()
                }
            }
        }
    }
}

@Composable
private fun FilterRowTextItems() {
    Row(Modifier
        .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        listOf("Chapters", "Volumes").forEach {
            Text(
                text = it,
                modifier = Modifier
                    .weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

