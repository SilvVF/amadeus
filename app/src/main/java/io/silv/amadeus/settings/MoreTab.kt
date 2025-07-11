package io.silv.amadeus.settings

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.twotone.Download
import androidx.compose.material.icons.twotone.History
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.QueryStats
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.icons.twotone.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabOptions
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Brands
import compose.icons.fontawesomeicons.brands.GithubSquare
import io.silv.manga.R
import io.silv.manga.download.DownloadQueueScreen
import io.silv.manga.history.RecentsScreen
import io.silv.manga.stats.StatsScreen
import io.silv.manga.storeage.StorageScreen
import io.silv.ui.LaunchedOnReselect
import io.silv.ui.ReselectTab
import io.silv.ui.openOnWeb
import io.silv.ui.theme.LocalSpacing
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel

object MoreTab: ReselectTab {
    private fun readResolve(): Any = this

    override val reselectCh: Channel<Unit> = Channel(1, BufferOverflow.DROP_OLDEST)

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 3u,
            title = "More",
            icon = rememberVectorPainter(image = Icons.Filled.MoreHoriz)
        )

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        LaunchedOnReselect {
            navigator.push(SettingsScreen())
        }

        MoreHomeScreen()
    }
}


@Composable
fun MoreHomeScreen() {
    val navigator = LocalNavigator.currentOrThrow
    val space = LocalSpacing.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Image(
            painter = painterResource(io.silv.data.R.drawable.amadeuslogo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(space.large)
                .fillMaxWidth(0.5f)
                .aspectRatio(1f)
                .clip(CircleShape)
                .align(Alignment.CenterHorizontally)
        )
        HorizontalDivider()
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
            navigator.push(StatsScreen())
        }
        MoreSelectionItem(
            title = "Storage",
            icon = Icons.TwoTone.Storage
        ) {
            navigator.push(StorageScreen())
        }
        HorizontalDivider()
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
        val space = LocalSpacing.current
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("About") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(space.large)
            ) {

                Image(
                    painter = painterResource(io.silv.data.R.drawable.amadeuslogo),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(space.large)
                        .fillMaxWidth(0.3f)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .align(Alignment.CenterHorizontally)
                )
                HorizontalDivider()
                Spacer(Modifier.height(space.xlarge))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            context
                                .openOnWeb("https://github.com/SilvVF/amadeus", "open on web")
                                .onFailure {
                                    Toast
                                        .makeText(
                                            context,
                                            "failed to open link",
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()
                                }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(
                        imageVector = FontAwesomeIcons.Brands.GithubSquare,
                        null,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(Modifier.width(space.med))
                    Text("View on GitHub")
                }
                Spacer(Modifier.height(space.large))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            context
                                .openOnWeb("https://tachiyomi.org", "open on web")
                                .onFailure {
                                    Toast
                                        .makeText(
                                            context,
                                            "failed to open link",
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()
                                }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.tachi_logo_128px),
                        null,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(Modifier.width(space.med))
                    Text("Inspired by Tachiyomi")
                }
                Spacer(Modifier.height(space.large))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            context
                                .openOnWeb("https://github.com/nekomangaorg/Neko", "open on web")
                                .onFailure {
                                    Toast
                                        .makeText(
                                            context,
                                            "failed to open link",
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()
                                }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.neko_icon),
                        null,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(Modifier.width(space.med))
                    Text("Inspired by Neko")
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
