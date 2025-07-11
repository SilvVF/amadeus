package io.silv.amadeus

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import io.silv.explore.ExploreTab
import io.silv.library.LibraryTab
import io.silv.manga.history.RecentsTab
import io.silv.amadeus.settings.MoreTab
import io.silv.ui.LocalAppState

object NavHost : Screen {

    private fun readResolve(): Any = this

    @Composable
    override fun Content() {
        val appState = LocalAppState.current
        val nav = LocalNavigator.current

        TabNavigator(
            tab = LibraryTab,
        ) { tabNavigator ->
            DisposableEffect(Unit) {
                appState.tabNavigator = tabNavigator
                onDispose { appState.tabNavigator = null }
            }

            CompositionLocalProvider(LocalNavigator provides nav) {
                NavigationSuiteScaffold(
                    navigationSuiteItems = {
                        listOf(LibraryTab, RecentsTab, ExploreTab, MoreTab)
                            .fastForEach {
                                item(
                                    modifier = Modifier,
                                    selected = tabNavigator.current == it,
                                    onClick = { appState.onTabSelected(it) },
                                    label = { Text(it.options.title) },
                                    icon = {
                                        Icon(
                                            painter = it.options.icon ?: return@item,
                                            contentDescription = it.options.title,
                                        )
                                    }
                                )
                            }
                    },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    AnimatedContent(
                        modifier = Modifier.fillMaxSize(),
                        targetState = tabNavigator.current,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "tabContent",
                    ) {
                        tabNavigator.saveableState(key = "currentTab", it) {
                            it.Content()
                        }
                    }
                }
            }
        }
    }
}
