package io.silv.amadeus.ui.screens.saved

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.FadeTransition
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.silv.amadeus.ui.composables.MangaListItem
import io.silv.amadeus.ui.composables.TranslatedLanguageTags
import io.silv.amadeus.ui.screens.home.vertical
import io.silv.amadeus.ui.screens.manga_view.MangaViewScreen
import io.silv.amadeus.ui.shared.CenterBox
import io.silv.amadeus.ui.shared.shadow
import io.silv.amadeus.ui.theme.LocalBottomBarVisibility
import io.silv.amadeus.ui.theme.LocalPaddingValues
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.domain.models.DomainChapter
import io.silv.manga.domain.models.DomainManga

object SavedTab: Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = "Saved"
            val icon = rememberVectorPainter(Icons.Filled.Bookmark)
            return remember {
                TabOptions(
                    index = 1u,
                    title = title,
                    icon = icon
                )
            }
        }


    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    override fun Content() {

        Navigator(SavedScreen()) {
            FadeTransition(it)
        }
    }
}

class SavedScreen: Screen {

    @Composable
    override fun Content() {
        val sm = getScreenModel<SavedMangaSM>()

        val bookmarked by sm.bookmarkedMangas.collectAsStateWithLifecycle()
        val saved by sm.savedMangas.collectAsStateWithLifecycle()
        val continueReading by sm.continueReading.collectAsStateWithLifecycle()

        Saved(
            bookmarked = bookmarked,
            saved = saved,
            continueReading = continueReading,
            bookmarkManga = {

            }
        )
    }
}

@Composable
fun Saved(
    bookmarked: List<Pair<DomainManga, List<DomainChapter>>>,
    saved: List<Pair<DomainManga, List<DomainChapter>>>,
    continueReading: List<Pair<DomainManga, List<DomainChapter>>>,
    bookmarkManga: (id: String) -> Unit
) {

    var bottomBarVisible by LocalBottomBarVisibility.current

    LaunchedEffect(Unit) {
        bottomBarVisible = true
    }

    val space = LocalSpacing.current
    val topLevelPadding by LocalPaddingValues.current
    val navigator = LocalNavigator.current

    Scaffold { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(topLevelPadding)
                .padding(paddingValues)
        ) {
            item {
                Text("Continue Reading", style = MaterialTheme.typography.headlineMedium)
                LazyRow {
                    items(continueReading) {(manga, chapters) ->
                        ReadingListItem(
                            manga = manga,
                            chapters = chapters
                        )
                    }
                }
            }
            item {
                Column {
                    Text("Bookmarked", style = MaterialTheme.typography.headlineMedium)
                    LazyRow {
                        itemsIndexed(bookmarked) { i, (manga, chapterInfo) ->
                            Row(
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                Text(
                                    modifier = Modifier
                                        .vertical()
                                        .rotate(-90f)
                                        .padding(space.small)
                                        .offset(x = 60.dp)
                                        .widthIn(0.dp, 240.dp),
                                    text = "${i + 1} ${manga.titleEnglish}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                )
                                MangaListItem(
                                    manga = manga,
                                    modifier = Modifier
                                        .padding(space.large)
                                        .width(240.dp)
                                        .height(290.dp)
                                        .clickable {
                                            navigator?.push(
                                                MangaViewScreen(manga)
                                            )
                                        },
                                    onBookmarkClick = { bookmarkManga(manga.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingListItem(
    modifier: Modifier = Modifier,
    manga: DomainManga,
    chapters: List<DomainChapter>
) {

    val space = LocalSpacing.current
    val context = LocalContext.current

    Column(
        modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(space.med)
    ) {
        CenterBox(
            modifier = Modifier
                .shadow(
                    color = Color.DarkGray,
                    blurRadius = 12.dp,
                    offsetY = space.xs,
                    offsetX = space.xs
                )
                .padding(space.xs)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(manga.coverArt)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(12.dp)
                    ),
                contentScale = ContentScale.Crop
            )
        }
        Text(
            text = manga.titleEnglish,
            style = MaterialTheme.typography.titleMedium
        )
        Divider()
        Row {
            val furthestChapter = remember {
                manga.readChapters.maxBy {id ->
                    chapters.find { it.id == id }?.chapter?.toIntOrNull() ?: -1
                }
            }
            Text("Chapter $furthestChapter / ${manga.lastChapter ?: chapters.size}")
            Divider()
        }
    }
}