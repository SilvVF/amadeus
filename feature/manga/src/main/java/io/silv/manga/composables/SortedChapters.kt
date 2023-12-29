package io.silv.manga.composables

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import io.silv.domain.chapter.model.Chapter

class SortedChapters(
    private val chapters: List<Chapter>,
    val sortBy: SortBy,
    private val onSortByChange: (SortBy) -> Unit,
) {
    enum class SortBy { Asc, Dsc }

    val sortedChapters by derivedStateOf {
        if (sortBy == SortBy.Asc) {
            chapters.sortedBy { it.chapter }
        } else {
            chapters.sortedByDescending { it.chapter }
        }
    }

    val sortedVolumes by derivedStateOf {
        val grouped = chapters.groupBy { it.volume }
        return@derivedStateOf if (sortBy == SortBy.Asc) {
            buildMap {
                grouped.keys.sorted().forEach {
                    put(it, (grouped[it]?.sortedBy { it.chapter } ?: emptyList<Chapter>()))
                }
            }
        } else {
            buildMap {
                grouped.keys.sortedDescending().forEach {
                    put(
                        it,
                        (grouped[it]?.sortedByDescending { it.chapter } ?: emptyList<Chapter>())
                    )
                }
            }
        }
    }

    fun sortBy(sortBy: SortBy) {
        onSortByChange(sortBy)
    }

    fun sortByOpposite() {
        if (sortBy == SortBy.Asc) {
            sortBy(SortBy.Dsc)
        } else {
            sortBy(SortBy.Asc)
        }
    }
}
