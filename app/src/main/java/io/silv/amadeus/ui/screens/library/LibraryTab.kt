package io.silv.amadeus.ui.screens.library

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
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
import io.silv.amadeus.ui.screens.manga_reader.ChapterList
import io.silv.amadeus.ui.screens.manga_reader.MangaReaderScreen
import io.silv.amadeus.ui.shared.CenterBox
import io.silv.amadeus.ui.shared.noRippleClickable
import io.silv.amadeus.ui.stateholders.rememberVolumeItemsState

object LibraryTab: Tab {

    //TODO(add ui to delete and filter downloaded manga)

    override val options: TabOptions
        @Composable
        get() {
            val title = "library"
            val icon = rememberVectorPainter(Icons.Default.LibraryBooks)

            return remember {
                TabOptions(
                    index = 2u,
                    title = title,
                    icon = icon
                )
            }
        }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    override fun Content() {
        Navigator(screen = LibraryScreen()) {
            FadeTransition(navigator = it)
        }
    }
}

class LibraryScreen: Screen {

    @Composable
    override fun Content() {
        val sm = getScreenModel<LibrarySM>()
        val navigator = LocalNavigator.current
        val mangasToChapters by sm.mangaWithDownloadedChapters.collectAsStateWithLifecycle()

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
        ) {
            items(
                items = mangasToChapters,
                key = { item -> item.first.id }
            ) { (manga, chapters) ->

                var expanded by rememberSaveable {
                    mutableStateOf(false)
                }

                val ctx = LocalContext.current
                val volumeItemsState = rememberVolumeItemsState(chapters = chapters)
                CenterBox(
                    Modifier.noRippleClickable { expanded = !expanded }
                ) {
                    if (expanded) {
                        ChapterList(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            volumeItemsState = volumeItemsState,
                            onChapterClicked = {
                                navigator?.push(
                                    MangaReaderScreen(
                                        it.mangaId, it.id
                                    )
                                )
                            }
                        )
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(ctx)
                                .data(manga.coverArt)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.height(300.dp)
                        )
                    }
                }
            }
        }
    }
}