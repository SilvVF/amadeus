package io.silv.amadeus.ui.screens.manga_view.composables

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import io.silv.manga.domain.models.DomainChapter
import io.silv.amadeus.ui.composables.AnimatedBoxShimmer
import io.silv.amadeus.ui.screens.manga_view.CoverArtState
import io.silv.amadeus.ui.shared.CenterBox
import io.silv.amadeus.ui.stateholders.VolumeItemsState
import io.silv.amadeus.ui.theme.LocalSpacing


@Composable
fun VolumeList(
    volumeItems: VolumeItemsState.VolumeItems,
    coverArtState: CoverArtState,
    onRetryLoadCoverArt: () -> Unit
) {
    when (volumeItems) {
        is VolumeItemsState.Volumes -> {

            if (volumeItems.items.isEmpty()) {
                return CenterBox(Modifier.fillMaxSize()) {
                    Text("No Volumes to display")
                }
            }

            var selectedVolume by rememberSaveable {
                mutableStateOf<List<DomainChapter>?>(null)
            }

            BackHandler(
                enabled = selectedVolume != null
            ) {
                selectedVolume = null
            }

            AnimatedVisibility(visible = selectedVolume != null) {
                selectedVolume?.let { VolumeView(Modifier.fillMaxSize(), it) }
            }
            VolumeImageGrid(
                coverArtState = coverArtState,
                volumeItems = volumeItems.items,
                onRetryLoadCoverArt = onRetryLoadCoverArt,
                onVolumeSelected = {
                    selectedVolume = it
                }
            )
        }
        else -> Unit
    }
}

@Composable
private fun VolumeView(
    modifier: Modifier = Modifier,
    volume: List<DomainChapter>
) {
    CenterBox(modifier) {
        Text(volume.toString())
    }
}

@Composable
private fun VolumeImageGrid(
    coverArtState: CoverArtState,
    volumeItems: List<List<DomainChapter>>,
    onRetryLoadCoverArt: () -> Unit,
    onVolumeSelected: (List<DomainChapter>) -> Unit
) {
    val ctx = LocalContext.current
    val space = LocalSpacing.current
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Fixed(2)
    ) {
        items(volumeItems) { volume ->
            when (coverArtState) {
                is CoverArtState.Failure -> CenterBox(Modifier.size(120.dp)) {
                    Text(text = "Failed to load")
                    Text(
                        text = "retry",
                        modifier = Modifier.clickable { onRetryLoadCoverArt() })
                }
                CoverArtState.Loading -> AnimatedBoxShimmer(Modifier.size(120.dp))
                is CoverArtState.Success -> {
                    Column {
                        coverArtState.art[volume.first().volume]?.let {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(ctx)
                                    .data(it)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                loading = { AnimatedBoxShimmer(Modifier.size(200.dp)) },
                                error = {
                                    CenterBox(
                                        Modifier
                                            .size(120.dp)
                                            .border(
                                                width = 2.dp,
                                                MaterialTheme.colorScheme.primary
                                            )
                                    ) {
                                        Text(
                                            "Volume #${volume.first().volume ?: "unknown"}",
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                },
                                modifier = Modifier.size(200.dp)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(space.large)
                        ) {
                            Column {
                                Text(text ="Volume #${volume.first().volume}")
                                Text(text ="Chapters: ${volume.size}")
                            }
                            IconButton(onClick = { onVolumeSelected(volume) }) {
                                Icon(imageVector = Icons.Filled.Info, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}