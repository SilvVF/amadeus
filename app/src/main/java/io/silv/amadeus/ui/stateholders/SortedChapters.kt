package io.silv.amadeus.ui.stateholders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.silv.manga.domain.models.DomainChapter

class SortedChapters(
    private val chapters: List<DomainChapter>,
    val sortBy: SortBy,
    private val onSortByChange: (SortBy) -> Unit,
) {

    enum class SortBy { Asc, Dsc }

    val sortedChapters by derivedStateOf {
        if (sortBy == SortBy.Asc) {
            chapters.sortedBy { it.chapter?.toDoubleOrNull() ?: 0.0 }
        } else {
            chapters.sortedByDescending { it.chapter?.toDoubleOrNull() ?: 0.0 }
        }
    }

    val sortedVolumes by derivedStateOf {
        val grouped = chapters.groupBy { it.volume?.toDoubleOrNull() ?: 0.0 }
        return@derivedStateOf if (sortBy == SortBy.Asc) {
            buildMap {
                grouped.keys.sorted().forEach {
                    put(it, (grouped[it]?.sortedBy { it.chapter?.toDoubleOrNull() ?: 0.0 } ?: emptyList<DomainChapter>()))
                }
            }
        } else {
            buildMap {
                grouped.keys.sortedDescending().forEach {
                    put(it, (grouped[it]?.sortedByDescending { it.chapter?.toDoubleOrNull() ?: 0.0 } ?: emptyList<DomainChapter>()))
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
    chapters: List<DomainChapter>,
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