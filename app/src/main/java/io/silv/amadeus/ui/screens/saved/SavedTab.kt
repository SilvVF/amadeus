package io.silv.amadeus.ui.screens.saved

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.FadeTransition
import io.silv.amadeus.ui.composables.MangaListItem
import io.silv.amadeus.ui.screens.manga_view.MangaViewScreen
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
    val paddingValues by LocalPaddingValues.current
    val navigator = LocalNavigator.current

    Scaffold { pad ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(pad)
        ) {
            item {
                Column(
                    Modifier
                        .height(400.dp)
                        .fillMaxWidth()
                ) {
                    Text(text = "Continue Reading")
                    LazyRow(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(
                            continueReading,
                            key = { item -> item.first.id }
                        ) { (manga, _) ->
                            MangaListItem(
                                manga = manga,
                                modifier = Modifier
                                    .width(300.dp)
                                    .padding(space.large)
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
            item {
                Column(
                    Modifier
                        .height(400.dp)
                        .fillMaxWidth()
                ) {
                    Text(text = "Bookmarked")
                    LazyRow(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(
                            bookmarked,
                            key = { item -> item.first.id }
                        ) { (manga, _) ->
                            MangaListItem(
                                manga = manga,
                                modifier = Modifier
                                    .width(300.dp)
                                    .padding(space.large)
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
            item {
                Column(
                    Modifier
                        .height(400.dp)
                        .fillMaxWidth()
                ) {
                    Text(text = "Saved")
                    LazyRow(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(
                            saved,
                            key = { item -> item.first.id }
                        ) { (manga, _) ->
                            MangaListItem(
                                manga = manga,
                                modifier = Modifier
                                    .width(300.dp)
                                    .padding(space.large)
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