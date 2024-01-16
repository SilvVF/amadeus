package io.silv.manga.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.twotone.Download
import androidx.compose.material.icons.twotone.History
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.QueryStats
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.icons.twotone.Storage
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabOptions
import io.silv.manga.download.DownloadQueueScreen
import io.silv.manga.history.RecentsScreen
import io.silv.ui.ReselectTab
import io.silv.ui.theme.LocalSpacing

object MoreTab: ReselectTab {

    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(SettingsScreen())
    }

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 3u,
            title = "More",
            icon = rememberVectorPainter(image = Icons.Filled.MoreHoriz)
        )

    @Composable
    override fun Content() {
        MoreHomeScreen()
    }
}


@Composable
fun MoreHomeScreen() {
    val navigator = LocalNavigator.currentOrThrow
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Icon(
            imageVector = Icons.Filled.PlayCircleOutline,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
        Divider()
        MoreSelectionItem(
            title = "History",
            icon = Icons.TwoTone.History
        ) {
            navigator.push(RecentsScreen())
        }
        MoreSelectionItem(
            title = "Download Queue",
            icon = Icons.TwoTone.Download
        ) {
            navigator.push(DownloadQueueScreen())
        }
        MoreSelectionItem(
            title = "Statistics",
            icon = Icons.TwoTone.QueryStats
        ) {

        }
        MoreSelectionItem(
            title = "Storage",
            icon = Icons.TwoTone.Storage
        ) {

        }
        Divider()
        MoreSelectionItem(
            title = "Settings",
            icon = Icons.TwoTone.Settings
        ) {
            navigator.push(SettingsScreen())
        }
        MoreSelectionItem(
            title = "About",
            icon = Icons.TwoTone.Info
        ) {
            navigator.push(AboutScreen())
        }
    }
}


class AboutScreen: Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("About") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(imageVector = Icons.Filled.ArrowBack, null)
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)) {
                Icon(
                    imageVector = Icons.Filled.PlayCircleOutline,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
                Divider()
                IconButton(onClick = { /*TODO*/ }) {

                }
            }
        }
    }
}

@Composable
fun MoreSelectionItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val space = LocalSpacing.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(space.large)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(space.large)
        )
    }
}
