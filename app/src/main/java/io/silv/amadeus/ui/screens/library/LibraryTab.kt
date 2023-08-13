package io.silv.amadeus.ui.screens.library

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import io.silv.amadeus.ui.screens.home.SearchTopAppBar
import io.silv.amadeus.ui.screens.manga_reader.MangaReaderScreen
import io.silv.amadeus.ui.screens.manga_view.MangaViewScreen
import io.silv.amadeus.ui.shared.CenterBox
import io.silv.amadeus.ui.shared.noRippleClickable
import io.silv.amadeus.ui.theme.LocalSpacing

object LibraryTab: Tab {

    @OptIn(ExperimentalAnimationGraphicsApi::class)
    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_library_enter)
            return TabOptions(
                index = 2u,
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
        val space = LocalSpacing.current


        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(state = rememberTopAppBarState())

        var searching by rememberSaveable {
            mutableStateOf(false)
        }

        var searchText by rememberSaveable {
            mutableStateOf("")
        }

        AmadeusScaffold(
            modifier = Modifier.fillMaxSize(),
            scrollBehavior = scrollBehavior,
            topBar = {
                SearchTopAppBar(
                    title = "Library",
                    scrollBehavior = scrollBehavior,
                    onSearchText = { searchText = it },
                    color = Color.Transparent,
                    navigationIconLabel = "",
                    navigationIcon = Icons.Filled.KeyboardArrowLeft,
                    onNavigationIconClicked = { searching = false },
                    actions = {},
                    searchText = searchText,
                    showTextField = searching ,
                    onSearchChanged = { searching = it },
                    onForceSearch = {}
                )
            }
        ) {
            LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
                    .padding(space.large),
                columns = GridCells.Fixed(2),
            ) {
                items(
                    items = mangasToChapters,
                    key = { item -> item.savableManga.id }
                ) {item ->
                    val (manga, chapters) = item
                    val ctx = LocalContext.current

                    CenterBox(
                        Modifier.padding(space.large)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(ctx)
                                .data(manga.coverArt)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .noRippleClickable {
                                    navigator?.push(
                                        MangaViewScreen(manga)
                                    )
                                }
                        )
                        FilterChip(
                            onClick = {},
                            selected = true,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(
                                    x = -(space.large),
                                    y = -(space.large)
                                ),
                            label = {
                                Text(
                                    text = item.unread.toString()
                                )
                            }
                        )
                        IconButton(
                            onClick = {
                                item.lastReadChapter?.let {
                                    navigator?.push(
                                        MangaReaderScreen(
                                            mangaId = manga.id,
                                            chapterId = it.id
                                        )
                                    )
                                }
                            },
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)

                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MenuBook,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }
}