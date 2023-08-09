package io.silv.amadeus.ui.screens.library

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.FadeTransition
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.silv.amadeus.AmadeusScaffold
import io.silv.amadeus.R
import io.silv.amadeus.ui.screens.manga_reader.ChapterList
import io.silv.amadeus.ui.screens.manga_reader.MangaReaderScreen
import io.silv.amadeus.ui.shared.CenterBox
import io.silv.amadeus.ui.shared.noRippleClickable
import io.silv.amadeus.ui.stateholders.rememberSortedChapters

object LibraryTab: Tab {

    @OptIn(ExperimentalAnimationGraphicsApi::class)
    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_library_enter)
            return TabOptions(
                index = 3u,
                title = "Library",
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val sm = getScreenModel<LibrarySM>()
        val navigator = LocalNavigator.current
        val mangasToChapters by sm.mangaWithDownloadedChapters.collectAsStateWithLifecycle()
        
        AmadeusScaffold(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize().padding(it),
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
                    val sortedChapters = rememberSortedChapters(chapters = chapters)
                    CenterBox(
                        Modifier.noRippleClickable { expanded = !expanded }
                    ) {
                        if (expanded) {
                            ChapterList(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                                sortedChapters = sortedChapters,
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
}