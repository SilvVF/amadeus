package io.silv.amadeus.ui.stateholders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.silv.manga.domain.models.SavableChapter

class SortedChapters(
    private val chapters: List<SavableChapter>,
    val sortBy: SortBy,
    private val onSortByChange: (SortBy) -> Unit,
) {

    enum class SortBy { Asc, Dsc }

    val sortedChapters by derivedStateOf {
        if (sortBy == SortBy.Asc) {
            chapters.sortedBy { it.chapter }
        } else {
            chapters.sortedByDescending { it.chapter}
        }
    }

    val sortedVolumes by derivedStateOf {
        val grouped = chapters.groupBy { it.volume }
        return@derivedStateOf if (sortBy == SortBy.Asc) {
            buildMap {
                grouped.keys.sorted().forEach {
                    put(it, (grouped[it]?.sortedBy { it.chapter } ?: emptyList<SavableChapter>()))
                }
            }
        } else {
            buildMap {
                grouped.keys.sortedDescending().forEach {
                    put(it, (grouped[it]?.sortedByDescending { it.chapter } ?: emptyList<SavableChapter>()))
                }
            }
        }
    }

    fun sortBy(sortBy: SortBy) {
        onSortByChange(sortBy)
    }

    fun sortByOpposite() {
        if (sortBy == SortBy.Asc) sortBy(SortBy.Dsc)
        else sortBy(SortBy.Asc)
    }
}

@Composable
fun rememberSortedChapters(
    chapters: List<SavableChapter>,
): SortedChapters {

    var sortBy by rememberSaveable {
        mutableStateOf(SortedChapters.SortBy.Asc)
    }

    return remember(chapters, sortBy) {
        SortedChapters(
            chapters, sortBy,
            onSortByChange = {
                sortBy = it
            }
        )
    }
}