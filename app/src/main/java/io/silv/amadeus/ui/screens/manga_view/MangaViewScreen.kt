package io.silv.amadeus.ui.screens.manga_view

import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import io.silv.amadeus.AmadeusScaffold
import io.silv.amadeus.ui.composables.MainPoster
import io.silv.amadeus.ui.screens.manga_filter.MangaFilterScreen
import io.silv.amadeus.ui.screens.manga_reader.MangaReaderScreen
import io.silv.amadeus.ui.screens.manga_view.composables.MangaContent
import io.silv.amadeus.ui.screens.manga_view.composables.WebViewOverlay
import io.silv.amadeus.ui.screens.manga_view.composables.chapterListItems
import io.silv.amadeus.ui.screens.manga_view.composables.volumePosterItems
import io.silv.amadeus.ui.shared.collectEvents
import io.silv.amadeus.ui.shared.isScrollingUp
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.domain.models.SavableManga
import kotlinx.parcelize.Parcelize
import org.koin.core.parameter.parametersOf



@Parcelize
class MangaViewScreen(
    private val manga: SavableManga
): Screen, Parcelable {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {

        val sm = getScreenModel<MangaViewSM> { parametersOf(manga) }
        val mangaViewState by sm.mangaViewStateUiState.collectAsStateWithLifecycle()
        val downloading by sm.downloadingOrDeleting.collectAsStateWithLifecycle()
        val navigator = LocalNavigator.current
        val statsUiState by sm.statsUiState.collectAsStateWithLifecycle()
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(state = rememberTopAppBarState())
        val listState = rememberLazyListState()
        val snackbarHostState = remember { SnackbarHostState() }

        sm.collectEvents { event ->
            suspend fun showSnackBar(message: String) {
                snackbarHostState.showSnackbar(
                    message = message,
                    withDismissAction = true,
                    duration = SnackbarDuration.Short
                )
            }
            when (event) {
                is MangaViewEvent.FailedToLoadChapterList -> showSnackBar(event.message)
                is MangaViewEvent.FailedToLoadVolumeArt -> showSnackBar(event.message)
            }
        }

        var showArtBottomSheet by rememberSaveable {
            mutableStateOf(false)
        }

        var webUrl by remember {
            mutableStateOf<String?>(null)
        }

        BackHandler(
            enabled = webUrl != null
        ) {
            webUrl = null
        }
        if (webUrl != null) {
            webUrl?.let {
                WebViewOverlay(
                    base = it
                )
            }
            return
        }

        if (showArtBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showArtBottomSheet = false },
                sheetState = rememberModalBottomSheetState(
                    skipPartiallyExpanded = true,
                ),

                ) {
                LazyColumn {
                    volumePosterItems(mangaViewState)
                }
            }
        }

        AmadeusScaffold(
            showBottomBar = false,
            scrollBehavior = scrollBehavior,
            topBar = {
                val bg = MaterialTheme.colorScheme.background
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = bg
                    ),
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = {
                            navigator?.pop()
                        }) {
                            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    scrollBehavior = scrollBehavior,
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.systemBars),
            floatingActionButton = {
                AnimatedVisibility(
                    visible = mangaViewState.chapters.fastAny { !it.read },
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    val space = LocalSpacing.current
                    ExtendedFloatingActionButton(
                        text = {
                            val text = remember(mangaViewState.chapters) {
                                if (mangaViewState.chapters.fastAny { it.started }) {
                                    "Resume"
                                } else {
                                    "Start"
                                }
                            }
                            Text(text = text)
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        icon = { Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null) },
                        onClick = {
                            val lastUnread = mangaViewState.chapters
                                .sortedBy { it.chapter.takeIf { it >= 0 } ?: Long.MAX_VALUE }
                                .fastFirstOrNull { !it.read }
                                ?: mangaViewState.chapters.minByOrNull { it.chapter } ?: return@ExtendedFloatingActionButton
                            navigator?.push(
                                MangaReaderScreen(
                                    mangaId = lastUnread.mangaId,
                                    chapterId = lastUnread.id
                                )
                            )
                        },
                        modifier = Modifier.padding(space.large),
                        expanded = listState.isScrollingUp(),
                    )
                }
            }
        ) { paddingValues ->
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    MainPoster(
                        manga = manga,
                        modifier = Modifier.fillMaxWidth(),
                        viewMangaArtClick = {
                            showArtBottomSheet = !showArtBottomSheet
                        },
                        statsState = statsUiState,
                        padding = paddingValues
                    )
                }
                item {
                    Column {
                        MangaContent(
                            manga = manga,
                            bookmarked = mangaViewState.manga.bookmarked,
                            onBookmarkClicked = sm::bookmarkManga,
                            onTagSelected = { tag ->
                                manga.tagToId[tag]?.let { id ->
                                    navigator?.push(
                                        MangaFilterScreen(tag, id)
                                    )
                                }
                            },
                            viewOnWebClicked = {
                                webUrl = "https://mangadex.org/title/${manga.id}"
                            }
                        )
                        FilterDropdownMenu(
                            modifier = Modifier.align(Alignment.End),
                            sortedByAsc = mangaViewState.sortedByAscending,
                            changeDirection = { sm.changeDirection() }
                        )
                    }
                }
                chapterListItems(
                    mangaViewState = mangaViewState,
                    downloadingIds = downloading,
                    onDownloadClicked = {
                        sm.downloadChapterImages(it)
                    },
                    asc = mangaViewState.sortedByAscending,
                    onDeleteClicked = {
                        sm.deleteChapterImages(listOf(it))
                    },
                    onReadClicked = {
                        navigator?.push(
                            MangaReaderScreen(manga.id, it)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun FilterDropdownMenu(
    modifier: Modifier = Modifier,
    sortedByAsc: Boolean,
    changeDirection: () -> Unit
) {
    var showingFilter by rememberSaveable {
        mutableStateOf(false)
    }
    val space = LocalSpacing.current
    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.small)) {
            Text("filter")
            IconButton(
                onClick = { showingFilter = !showingFilter },
                Modifier
            ) {
                Icon(
                    imageVector = Icons.Filled.FilterList,
                    contentDescription = null
                )
            }
        }
        DropdownMenu(
            expanded = showingFilter,
            onDismissRequest = { showingFilter = false },

            ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = if (sortedByAsc) "ascending" else "descending"
                    )
                },
                onClick = changeDirection,
            )
        }
    }
}



