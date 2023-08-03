package io.silv.amadeus.ui.screens.manga_view

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import io.silv.amadeus.ui.composables.MainPoster
import io.silv.amadeus.ui.composables.TranslatedLanguageTags
import io.silv.amadeus.ui.screens.manga_filter.MangaFilterScreen
import io.silv.amadeus.ui.screens.manga_reader.MangaReaderScreen
import io.silv.amadeus.ui.screens.manga_view.composables.ChapterListHeader
import io.silv.amadeus.ui.screens.manga_view.composables.ChapterVolumeNavBar
import io.silv.amadeus.ui.screens.manga_view.composables.MangaInfo
import io.silv.amadeus.ui.screens.manga_view.composables.chapterListItems
import io.silv.amadeus.ui.screens.manga_view.composables.volumePosterItems
import io.silv.amadeus.ui.shared.CenterBox
import io.silv.amadeus.ui.theme.LocalBottomBarVisibility
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.domain.models.DomainManga
import org.koin.core.parameter.parametersOf


class MangaViewScreen(
  private val manga: DomainManga
): Screen {

    @Composable
    override fun Content() {

        val sm = getScreenModel<MangaViewSM> { parametersOf(manga) }
        var bottomBarVisible by LocalBottomBarVisibility.current
        val mangaViewState by sm.mangaViewStateUiState.collectAsStateWithLifecycle()
        val downloading by sm.downloadingOrDeleting.collectAsStateWithLifecycle()
        val space = LocalSpacing.current
        val navigator = LocalNavigator.current
        val currentPage by sm.currentPage.collectAsStateWithLifecycle()
        val sortedAscending by sm.sortedByAsc.collectAsStateWithLifecycle()

        val snackbarHostState = remember { SnackbarHostState() }

        var chaptersShowing by rememberSaveable {
            mutableStateOf(true)
        }

        LaunchedEffect(Unit) { bottomBarVisible = false }

        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.systemBars)
        ) { paddingValues ->
            Column(Modifier
                    .padding(paddingValues)
                ) {
                LazyColumn(Modifier.fillMaxSize().navigationBarsPadding()) {
                    item {
                        MainPoster(
                            manga = manga,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Column {
                            MangaInfo(
                                manga = manga,
                                bookmarked = (mangaViewState.mangaState as? MangaState.Success)?.manga?.bookmarked ?: manga.bookmarked,
                                onBookmarkClicked = sm::bookmarkManga,
                                onTagSelected = {tag ->
                                    manga.tagToId[tag]?.let {id ->
                                        navigator?.push(
                                            MangaFilterScreen(tag, id)
                                        )
                                    }
                                }
                            )
                            ChapterVolumeNavBar(
                                chaptersShowing = chaptersShowing,
                                onChange = {
                                    chaptersShowing = it
                                }
                            )
                        }
                    }
                    if (chaptersShowing) {
                        item {
                            ChapterListHeader(
                                onPageClick = sm::navigateToPage,
                                page = currentPage + 1,
                                lastPage = mangaViewState.chapterPageState.lastPage,
                                sortedAscending = sortedAscending,
                                onChangeDirection = sm::changeDirection
                            )
                        }
                        chapterListItems(
                            chapterPageState = mangaViewState.chapterPageState,
                            downloadingIds = downloading,
                            onDownloadClicked = {
                                sm.downloadChapterImages(listOf(it))
                            },
                            onDeleteClicked = {
                                sm.deleteChapterImages(listOf(it))
                            },
                            onReadClicked = {
                                navigator?.push(
                                    MangaReaderScreen(manga.id, it)
                                )
                            }
                        )
                    }else {
                        volumePosterItems(mangaViewState.mangaState)
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsAndLanguages(
    manga: DomainManga,
    navigate: (name: String) -> Unit,
) {
    val space = LocalSpacing.current
    val list = remember(manga) {
        manga.tagToId.keys.toList()
    }
    var expanded by rememberSaveable {
        mutableStateOf(list.size < 4)
    }
    Text("Tags", style = MaterialTheme.typography.labelSmall)
    FlowRow {
        if (!expanded) {
            list.runCatching { take(3) }.getOrDefault(list).forEach {name ->
                AssistChip(
                    onClick = { navigate(name)},
                    label = { Text(name) },
                    modifier = Modifier.padding(horizontal = space.xs)
                )
            }
            if (list.size > 4) {
                AssistChip(
                    onClick = { expanded = true },
                    label = { Text("+ ${list.size - 3} more") },
                    modifier = Modifier.padding(horizontal = space.xs)
                )
            }
        } else {
            list.forEach { name ->
                AssistChip(
                    onClick = { navigate(name) },
                    label = { Text(name) },
                    modifier = Modifier.padding(horizontal = space.xs)
                )
            }
            if (list.size > 4) {
                IconButton(onClick = { expanded = false }) {
                    Icon(imageVector = Icons.Filled.KeyboardArrowLeft, contentDescription = null)
                }
            }
        }
    }
    Text("Translated Languages", style = MaterialTheme.typography.labelSmall)
    TranslatedLanguageTags(tags = manga.availableTranslatedLanguages)
}

@Composable
fun Pagination(
    modifier: Modifier = Modifier,
    page: Int,
    lastPage: Int,
    onPageClick: (page: Int) -> Unit,
) {

    val pagesTillEnd = remember(page, lastPage) { lastPage - page }

    var enteringPageLeft by remember {
        mutableStateOf(false)
    }
    var enteringPageRight by remember {
        mutableStateOf(false)
    }

    Row(modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = { onPageClick(page - 1) }) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = null
            )
        }
        PageItem(
            modifier = Modifier.size(32.dp),
            onClick = { onPageClick(1) },
            page = 1,
            selected = page == 1
        )
        if (page >= 4) {
            Box {
                IconButton(onClick = {
                    enteringPageRight = false
                    enteringPageLeft = true
                }) {
                    Icon(
                        imageVector = Icons.Filled.MoreHoriz,
                        contentDescription = null
                    )
                }
                DropdownMenu(
                    modifier = Modifier.heightIn(0.dp, 200.dp),
                    expanded = enteringPageLeft,
                    onDismissRequest = { enteringPageLeft = false }
                ) {
                    for (i in 1..lastPage) {
                        DropdownMenuItem(
                            text = { Text(i.toString()) },
                            onClick = {
                                onPageClick(i)
                                enteringPageLeft = false
                            }
                        )
                    }
                }
            }
        }
        for (i in (page - 2 until page).toList().filter { it > 1 }) {
            PageItem(
                modifier = Modifier.size(32.dp),
                onClick = { onPageClick(i) },
                page = i,
                selected = page == i
            )
        }
        if(page != 1 && page != lastPage) {
            PageItem(
                modifier = Modifier.size(32.dp),
                onClick = {},
                page = page,
                selected = true
            )
        }
        for (i in (page + 1..page + 2).toList().filter { it < lastPage }) {
            PageItem(
                modifier = Modifier.size(32.dp),
                onClick = { onPageClick(i) },
                page = i,
                selected = page == i
            )
        }
        if (pagesTillEnd >= 4) {
            Box {
                IconButton(onClick = {
                    enteringPageLeft = false
                    enteringPageRight = true
                }) {
                    Icon(
                        imageVector = Icons.Filled.MoreHoriz,
                        contentDescription = null
                    )
                }
                DropdownMenu(
                    modifier = Modifier.heightIn(0.dp, 200.dp),
                    expanded = enteringPageRight,
                    onDismissRequest = { enteringPageRight = false }
                ) {
                    for (i in 1..lastPage) {
                        DropdownMenuItem(
                            text = { Text(i.toString()) },
                            onClick = {
                                onPageClick(i)
                                enteringPageRight = false
                            }
                        )
                    }
                }
            }
        }
        if (lastPage != 1 && lastPage != 0) {
            PageItem(
                modifier = Modifier.size(32.dp),
                onClick = { onPageClick(lastPage) },
                page = lastPage,
                selected = page == lastPage
            )
        }
        IconButton(onClick = { onPageClick(page + 1)}) {
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = null
            )
        }
    }
}

@Composable
fun PageItem(
    modifier: Modifier,
    onClick: () -> Unit,
    page: Int,
    selected: Boolean
) {
    val background by animateColorAsState(
        targetValue = if(selected)
            MaterialTheme.colorScheme.primary
        else
            Color.Transparent,
        label = "page item background"
    )
    CenterBox(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable { onClick() }
    ) {
        Text(text = "$page")
    }
}

