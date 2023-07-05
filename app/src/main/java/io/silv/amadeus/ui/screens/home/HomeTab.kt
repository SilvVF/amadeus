package io.silv.amadeus.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import io.silv.amadeus.ui.composables.AnimatedBoxShimmer
import io.silv.amadeus.ui.composables.HomeTopBar
import io.silv.amadeus.ui.composables.MangaListItem
import io.silv.amadeus.ui.screens.manga_view.MangaViewScreen
import io.silv.amadeus.ui.theme.LocalBottomBarVisibility
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.domain.models.DomainManga

object HomeTab: Tab {

    override val options: TabOptions
        @Composable
        get() {
            val title = "Home"
            val icon = rememberVectorPainter(Icons.Filled.Home)

            return remember {
                TabOptions(
                    index = 0u,
                    title = title,
                    icon = icon
                )
            }
        }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    override fun Content() {
        Navigator(HomeScreen()) {
            FadeTransition(navigator = it)
        }
    }
}



