package io.silv.amadeus.ui.screens.manga_view.composables

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.silv.amadeus.domain.models.DomainChapter
import io.silv.amadeus.ui.stateholders.VolumeItemsState


@Composable
fun ChapterList(
    volumeItems: VolumeItemsState.VolumeItems,
) {
    when (volumeItems) {
        is VolumeItemsState.Chapters -> {
            LazyColumn(Modifier.fillMaxSize()) {
                items(
                    volumeItems.items,
                ) { chapter ->
                   ChapterListItem(chapter)
                }
            }
        }
        else -> Unit
    }
}

@Composable
private fun ChapterListItem(
    chapter: DomainChapter,
    modifier: Modifier = Modifier,
) {
    Text(text = "Chapter #${chapter.chapter} ${chapter.title}")
}