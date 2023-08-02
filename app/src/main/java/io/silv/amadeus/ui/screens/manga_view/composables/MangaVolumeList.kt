package io.silv.amadeus.ui.screens.manga_view.composables

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.silv.amadeus.ui.composables.AnimatedBoxShimmer
import io.silv.amadeus.ui.screens.home.header
import io.silv.amadeus.ui.screens.manga_view.CoverArtState
import io.silv.amadeus.ui.shared.CenterBox
import io.silv.amadeus.ui.stateholders.SortedChapters
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.domain.models.DomainChapter


@Composable
fun VolumeList(
    volumeItems: Map<Double, List<DomainChapter>>,
    sortBy: SortedChapters.SortBy,
    onSortByChange: () -> Unit,
    coverArtState: CoverArtState,
    onRetryLoadCoverArt: () -> Unit
) {
    if (volumeItems.isEmpty()) {
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
        volumeItems = volumeItems,
        onRetryLoadCoverArt = onRetryLoadCoverArt,
        onVolumeSelected = {
            selectedVolume = it
        },
        sortBy = sortBy,
        onSortByChange = onSortByChange
    )
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
    volumeItems: Map<Double, List<DomainChapter>>,
    onRetryLoadCoverArt: () -> Unit,
    sortBy: SortedChapters.SortBy,
    onSortByChange: () -> Unit,
    onVolumeSelected: (List<DomainChapter>) -> Unit
) {
    val ctx = LocalContext.current
    val space = LocalSpacing.current

    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Fixed(2)
    ) {
        header {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row( verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "sorting by: ${
                        when (sortBy) {
                            SortedChapters.SortBy.Asc -> "Ascending"
                            SortedChapters.SortBy.Dsc -> "Descending"
                        }}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    IconButton(onClick = onSortByChange) {
                        Icon(
                            imageVector = if (sortBy == SortedChapters.SortBy.Dsc)
                                Icons.Filled.KeyboardArrowDown
                            else
                                Icons.Filled.KeyboardArrowUp,
                            contentDescription = null
                        )
                    }
                }
            }
        }
        items(volumeItems.entries.toList()) {(num, chapters) ->
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
                        coverArtState.art[chapters.first().volume]?.let {
                            AsyncImage(
                                model = ImageRequest.Builder(ctx)
                                    .data(it)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
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
                                Text(text ="Volume #${chapters.first().volume}")
                                Text(text ="Chapters: ${chapters.size}")
                            }
                            IconButton(onClick = { onVolumeSelected(chapters) }) {
                                Icon(imageVector = Icons.Filled.Info, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}