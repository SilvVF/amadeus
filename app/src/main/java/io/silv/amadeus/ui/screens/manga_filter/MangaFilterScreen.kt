package io.silv.amadeus.ui.screens.manga_filter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import io.silv.amadeus.ui.composables.MangaListItem
import io.silv.amadeus.ui.screens.manga_view.MangaViewScreen
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.domain.models.DomainManga
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

class MangaFilterScreen(
    private val tag: String,
    private val tagId: String
) : Screen {

    override val key: ScreenKey
        get() = super.key + tag + tagId

    @Composable
    override fun Content() {

        val sm = getScreenModel<MangaFilterSM> { parametersOf(tagId) }
        val navigator = LocalNavigator.current
        val searchItemsState by sm.filteredUiState.collectAsStateWithLifecycle()
        val lazyGridState = rememberLazyGridState()

        LaunchedEffect(Unit) {
            launch {
                snapshotFlow { lazyGridState.canScrollForward }.collect {
                    if (!it) {
                        sm.loadNextPage()
                    }
                }
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            Text(text = "showing manga with tag $tag")
            val space = LocalSpacing.current
            LazyVerticalGrid(
                modifier = Modifier.weight(1f),
                state = lazyGridState,
                columns = GridCells.Fixed(2)
            ) {
                items(
                    items = searchItemsState,
                    key = { item: DomainManga -> item.id }
                ) { manga ->
                    MangaListItem(
                        manga = manga,
                        modifier = Modifier
                            .padding(space.large)
                            .clickable {
                                navigator?.push(
                                    MangaViewScreen(manga)
                                )
                            },
                        onTagClick = { name ->
                            manga.tagToId[name]?.let {
                                if (name != tag && it != tagId) {
                                    navigator?.replace(
                                        MangaFilterScreen(name, it)
                                    )
                                }
                            }
                        },
                        onBookmarkClick = {
                            sm.bookmarkManga(manga.id)
                        }
                    )
                }
            }
        }
    }
}