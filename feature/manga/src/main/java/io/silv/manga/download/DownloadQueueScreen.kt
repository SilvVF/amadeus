package io.silv.manga.download

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import io.silv.common.model.Download
import io.silv.ui.theme.LocalSpacing
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

class DownloadQueueScreen: Screen {
    @Composable
    override fun Content() {
        val screenModel = getScreenModel<DownloadQueueScreenModel>()

        val downloads by screenModel.state.collectAsStateWithLifecycle()
        val running by screenModel.isDownloaderRunning.collectAsStateWithLifecycle()

        DownloadScreenContent(
            downloads = downloads,
            actions = DownloadQueueActions(
                reorder = screenModel::reorder,
                cancel = screenModel::cancel,
                cancelAll = screenModel::clearQueue,
                cancelAllForSeries = {download ->
                     val toCancel = downloads
                         .fastFilter { it.download.manga.id == download.manga.id }
                         .map { it.download }

                    screenModel::cancel.invoke(toCancel)
                },
                pause = screenModel::pauseDownloads,
                play = screenModel::startDownloads
            ),
            running = running
        )
    }
}

data class DownloadQueueActions(
    val reorder: (downloads: List<Download>) -> Unit,
    val cancel: (download: List<Download>) -> Unit,
    val cancelAllForSeries: (download: Download) -> Unit,
    val pause: () -> Unit,
    val cancelAll: () -> Unit,
    val play: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadScreenContent(
    downloads: ImmutableList<DownloadItem>,
    actions: DownloadQueueActions,
    running: Boolean
) {
    Scaffold(
       topBar = {
           TopAppBar(
               navigationIcon = {
                   IconButton(
                       onClick = {  }
                   ) {
                       Icon(
                           imageVector = Icons.Filled.ArrowBack,
                           contentDescription = null)

                   }
               },
               title = { Text("Download queue") },
               actions = {
                   val space = LocalSpacing.current
                   val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
                   if (downloads.isNotEmpty()) {
                       Text(
                           text = "${downloads.size}",
                           modifier = Modifier.drawBehind {
                               drawCircle(
                                   color = surfaceVariant
                               )
                           }
                       )
                       Box {
                           var dropdownExpanded by remember { mutableStateOf(false) }
                           DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                               DropdownMenuItem(
                                   text = { Text("Cancel all") },
                                   onClick = { actions.cancelAll() }
                               )
                           }
                           IconButton(
                               onClick = {
                                   dropdownExpanded = true
                               }
                           ) {
                               Icon(
                                   imageVector = Icons.Default.MoreVert,
                                   contentDescription = null,
                                   modifier = Modifier.padding(space.small),
                                   tint = MaterialTheme.colorScheme.surfaceTint
                               )
                           }
                       }
                   }
               }
           )
       },
        floatingActionButton = {
            val (icon, action, text) = if(!running) {
                Triple(Icons.Filled.PlayArrow, actions.play, "Start")
            } else {
                Triple(Icons.Filled.Pause, actions.pause, "Pause")
            }
            FloatingActionButton(onClick = action) {
                Row {
                    Icon(imageVector = icon, contentDescription = null)
                    Text(text)
                }
            }
        }
    ) { paddingValues ->
        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column {
                    Text(text = "(uwu)", style = MaterialTheme.typography.titleLarge)
                    Text(text = "No downloads", style = MaterialTheme.typography.labelMedium)

                }
            }
        } else {
            VerticalReorderList(
                downloads = downloads,
                actions = actions,
                paddingValues
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VerticalReorderList(
    downloads: ImmutableList<DownloadItem>,
    actions: DownloadQueueActions,
    paddingValues: PaddingValues,
) {
    var data by remember(downloads) {
        mutableStateOf(
            downloads.map { it.download }
        )
    }

    val view = LocalView.current
    val haptics = LocalHapticFeedback.current
    val space = LocalSpacing.current

    val state = rememberReorderableLazyListState(
        onDragEnd = { from, to ->
            if(checkAtLeastApi30()) {
                view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
            }
            actions.reorder(data)
        },
        onMove = { from, to ->
            data = data.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
                .toImmutableList()

            if (checkAtLeastApi34()) {
                view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
            } else {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    )


    Surface {
        LazyColumn(
            state = state.listState,
            contentPadding = paddingValues,
            modifier = Modifier
                .reorderable(state)
                .detectReorderAfterLongPress(state),
        ) {
            items(
                items = data,
                key = { it.chapter.id }
            ) { item ->
                ReorderableItem(
                    state = state,
                    key = item.chapter.id,
                    modifier = Modifier
                ) { isDragging ->

                    val progress by item.progressFlow.collectAsStateWithLifecycle(0L)

                    val primaryColor = MaterialTheme.colorScheme.primary

                    val pages by produceState(initialValue = 0 to (item.pages?.size ?: 0)) {
                        while (true) {
                            value = item.downloadedImages to (item.pages?.size ?: 0)
                            delay(50)
                        }
                    }

                    val elevation by animateDpAsState(
                        if (isDragging) 4.dp else 0.dp,
                        label = "elevation-anim"
                    )

                    Surface(
                        shadowElevation = elevation,
                        modifier = Modifier.padding(space.med),
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .detectReorderAfterLongPress(state),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(space.small)
                                    .detectReorder(state),
                                tint = MaterialTheme.colorScheme.surfaceTint
                            )

                            Column(Modifier.weight(1f)) {
                                Row (Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                     Column {
                                         Text(item.manga.title, style = MaterialTheme.typography.titleMedium)
                                         Spacer(Modifier.height(space.xs))
                                         Text(
                                             remember(item) {
                                                 "Vol. ${item.chapter.volume} Ch. ${item.chapter.chapter} - ${item.chapter.title}"
                                             },
                                             style = MaterialTheme.typography.labelMedium
                                         )
                                     }
                                    Text("${pages.first} / ${pages.second}", style = MaterialTheme.typography.labelSmall)
                                }

                                val widthPct by animateFloatAsState(
                                    targetValue = progress.toFloat(),
                                    label = "progress-anim"
                                )

                                Canvas(
                                    modifier = Modifier
                                        .padding(space.med)
                                        .height(8.dp)
                                        .fillMaxWidth()
                                        .clip(CircleShape)
                                ) {
                                    drawRoundRect(
                                        color = primaryColor,
                                        size = Size(this.size.width * (widthPct / 100), this.size.height)
                                    )
                                }
                            }

                            var dropdownExpanded by remember { mutableStateOf(false) }

                            Box {
                                DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Move series to top") },
                                        onClick = {
                                            actions.reorder(
                                                buildSet {
                                                    add(item)
                                                    addAll(data)
                                                }
                                                    .toList()
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Move series to bottom") },
                                        onClick = {
                                            actions.reorder(
                                                buildSet {
                                                    addAll(data - item)
                                                    add(item)
                                                }
                                                    .toList()
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Cancel") },
                                        onClick = { actions.cancel(listOf(item)) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Cancel all for this series") },
                                        onClick = { actions.cancelAllForSeries(item) }
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        dropdownExpanded = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = null,
                                        modifier = Modifier.padding(space.small),
                                        tint = MaterialTheme.colorScheme.surfaceTint
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
fun checkAtLeastApi34(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
fun checkAtLeastApi30(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
}
