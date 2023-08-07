package io.silv.amadeus.ui.screens.manga_view

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.material3.SnackbarDuration
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
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import io.silv.amadeus.ui.composables.MainPoster
import io.silv.amadeus.ui.composables.TranslatedLanguageTags
import io.silv.amadeus.ui.screens.manga_filter.MangaFilterScreen
import io.silv.amadeus.ui.screens.manga_reader.MangaReaderScreen
import io.silv.amadeus.ui.screens.manga_view.composables.ChapterListHeader
import io.silv.amadeus.ui.screens.manga_view.composables.ChapterVolumeNavBar
import io.silv.amadeus.ui.screens.manga_view.composables.MangaContent
import io.silv.amadeus.ui.screens.manga_view.composables.chapterListItems
import io.silv.amadeus.ui.screens.manga_view.composables.volumePosterItems
import io.silv.amadeus.ui.shared.CenterBox
import io.silv.amadeus.ui.shared.Language
import io.silv.amadeus.ui.shared.collectEvents
import io.silv.amadeus.ui.theme.LocalBottomBarVisibility
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.domain.models.SavableManga
import org.koin.core.parameter.parametersOf

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoadWebViewUrls(
    base: String,
) {
    val webviewState = rememberWebViewState(url = base)

    WebView(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        state = webviewState,
        onCreated = {
            it.settings.javaScriptEnabled = true
            it.settings.blockNetworkImage = false
            it.settings.javaScriptCanOpenWindowsAutomatically = true
            it.settings.blockNetworkLoads = false
            it.settings.loadsImagesAutomatically = true
            it.settings.userAgentString = "Mozilla"
            it.settings.domStorageEnabled = true
        },
    )
}

class MangaViewScreen(
  private val manga: SavableManga
): Screen {

    @Composable
    override fun Content() {

        val sm = getScreenModel<MangaViewSM> { parametersOf(manga) }
        var bottomBarVisible by LocalBottomBarVisibility.current
        val mangaViewState by sm.mangaViewStateUiState.collectAsStateWithLifecycle()
        val downloading by sm.downloadingOrDeleting.collectAsStateWithLifecycle()
        val navigator = LocalNavigator.current
        val currentPage by sm.currentPage.collectAsStateWithLifecycle()
        val sortedAscending by sm.sortedByAsc.collectAsStateWithLifecycle()
        val selectedLanguages by sm.languageList.collectAsStateWithLifecycle()
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

        var chaptersShowing by rememberSaveable {
            mutableStateOf(true)
        }

        LaunchedEffect(Unit) {
            bottomBarVisible = false
        }

        var webUrl by remember {
            mutableStateOf<String?>(null)
        }

        BackHandler(
            enabled = webUrl != null
        ) {
            webUrl = null
        }

        webUrl?.let {
            LoadWebViewUrls(
                base = it,
            )
            return
        }


        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.systemBars)
        ) { paddingValues ->
                Column(
                    Modifier
                        .padding(paddingValues)
                ) {
                    LazyColumn(
                        Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                    ) {
                        item {
                            MainPoster(
                                manga = manga,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            Column {
                                MangaContent(
                                    manga = manga,
                                    bookmarked = mangaViewState.mangaState.manga.bookmarked,
                                    onBookmarkClicked = sm::bookmarkManga,
                                    onTagSelected = { tag ->
                                        manga.tagToId[tag]?.let { id ->
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
                                    onChangeDirection = sm::changeDirection,
                                    onLanguageSelected = sm::languageChanged,
                                    selectedLanguages = remember(selectedLanguages) {
                                        Language.values().filter { it.code in selectedLanguages }
                                    }
                                )
                            }
                            chapterListItems(
                                chapterPageState = mangaViewState.chapterPageState,
                                downloadingIds = downloading,
                                onDownloadClicked = {
                                    sm.downloadChapterImages(it)
                                },
                                onDeleteClicked = {
                                    sm.deleteChapterImages(listOf(it))
                                },
                                onOpenWebView = {
                                    webUrl = it
                                },
                                onReadClicked = {
                                    navigator?.push(
                                        MangaReaderScreen(manga.id, it)
                                    )
                                }
                            )
                        } else {
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
    manga: SavableManga,
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

