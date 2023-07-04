package io.silv.amadeus.ui.screens.manga_view

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import com.skydoves.orbital.Orbital
import com.skydoves.orbital.animateMovement
import com.skydoves.orbital.rememberContentWithOrbitalScope
import io.silv.manga.domain.models.DomainChapter
import io.silv.manga.domain.models.DomainManga
import io.silv.amadeus.ui.composables.AnimatedShimmer
import io.silv.amadeus.ui.composables.MangaViewPoster
import io.silv.amadeus.ui.screens.manga_view.composables.ChapterList
import io.silv.amadeus.ui.screens.manga_view.composables.VolumeList
import io.silv.amadeus.ui.shared.CenterBox
import io.silv.amadeus.ui.shared.StringStateListSaver
import io.silv.amadeus.ui.shared.collectEvents
import io.silv.amadeus.ui.shared.noRippleClickable
import io.silv.amadeus.ui.stateholders.VolumeItemsState
import io.silv.amadeus.ui.stateholders.rememberVolumeItemsState
import io.silv.amadeus.ui.theme.LocalBottomBarVisibility
import io.silv.amadeus.ui.theme.LocalSpacing
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf


class MangaViewScreen(
  private val manga: DomainManga
): Screen {

    @Composable
    override fun Content() {

        val sm = getScreenModel<MangaViewSM> { parametersOf(manga) }

        var bottomBarVisible by LocalBottomBarVisibility.current

        val state by sm.chapterInfoUiState.collectAsStateWithLifecycle()

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
                   onReadNowClick = {},
                   onBookMarkClick = {}
               )
               MangaView(
                   state = state.chapterListState,
                   coverArtState = state.coverArtState,
                   downloads = emptyList(),
                   retryLoadCoverArt = {
                     //  sm.loadVolumeCoverArt(manga.id, manga.lastVolume)
                   },
                   retryLoadChapterList = {
                   //    sm.loadMangaInfo(manga.id, manga.lastChapter)
                   },
                   downloadChapter = {
//                        sm.downloadChapter(it)
                   }
               )
            }
        }
    }
}

@Composable
fun MangaView(
    state: ChapterListState,
    coverArtState: CoverArtState,
    downloads: List<String>,
    retryLoadCoverArt: () -> Unit,
    retryLoadChapterList: () -> Unit,
    downloadChapter: (DomainChapter) -> Unit,
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

            val volumeItemsState = rememberVolumeItemsState(chapters = state.chapters)

            Column(Modifier.fillMaxSize()) {
                MangaViewFilterRow(volumeItemsState,
                    Modifier
                        .padding(vertical = space.med)
                        .fillMaxWidth()
                        .height(40.dp))
                when (volumeItemsState.items) {
                    is VolumeItemsState.Chapters -> {
                        ChapterList(
                            volumeItemsState.items,
                            volumeItemsState.sortBy,
                            downloads,
                            sortByChange = { volumeItemsState.sortByOpposite() },
                            downloadChapterClicked = downloadChapter
                        )
                    }
                    is VolumeItemsState.Volumes -> {
                        VolumeList(volumeItemsState.items, coverArtState) {
                            retryLoadCoverArt()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MangaViewFilterRow(
    volumeItemsState: VolumeItemsState,
    modifier: Modifier = Modifier
) {
    var showSortByMenu by rememberSaveable { mutableStateOf(false) }

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
                .noRippleClickable { volumeItemsState.groupByOpposite() }
                .fillMaxWidth()
        ) {
            if (volumeItemsState.groupBy == VolumeItemsState.GroupBy.Chapter) {
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

