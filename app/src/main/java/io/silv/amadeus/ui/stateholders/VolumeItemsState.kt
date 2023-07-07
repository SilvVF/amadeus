package io.silv.amadeus.ui.stateholders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.silv.manga.domain.models.DomainChapter

class VolumeItemsState(
    chapters: List<DomainChapter>,
    val sortBy: SortBy,
    val groupBy: GroupBy,
    private val onGroupByChange: (GroupBy) -> Unit,
    private val onSortByChange: (SortBy) -> Unit,
) {

    enum class GroupBy {
        Volume, Chapter
    }

    enum class SortBy {
        Asc, Dsc
    }

    sealed interface VolumeItems

    data class Chapters(val items: List<DomainChapter>): VolumeItems

    data class Volumes(val items: List<List<DomainChapter>>): VolumeItems

    private fun List<DomainChapter>.groupByVolumeSorted(
        sortBy: SortBy
    ) = when(sortBy){
        SortBy.Asc -> this.groupBy {
            it.volume?.toDoubleOrNull()?: 0.0
        }
            .values
            .toList()

        SortBy.Dsc -> this.groupBy {
            it.volume?.toDoubleOrNull()?: 0.0
        }
            .values
            .reversed()
    }

    val items: VolumeItems = when(groupBy) {
        GroupBy.Volume -> {
            Volumes(
                items = chapters.groupByVolumeSorted(sortBy).map { volume ->
                    when (sortBy) {
                        SortBy.Dsc -> volume.sortedByDescending { it.chapter?.toDoubleOrNull() ?: 0.0 }
                        SortBy.Asc -> volume.sortedBy { it.chapter?.toDoubleOrNull() ?: 0.0  }
                    }
                }
            )
        }
        GroupBy.Chapter -> {
            Chapters(
                items = when(sortBy) {
                    SortBy.Asc -> chapters.sortedBy { it.chapter?.toDoubleOrNull() ?: 0.0 }
                    SortBy.Dsc -> chapters.sortedByDescending { it.chapter?.toDoubleOrNull() ?: 0.0 }
                }
            )
        }
    }

    fun sortBy(sortBy: SortBy) {
        onSortByChange(sortBy)
    }

    fun sortByOpposite() {
        if (sortBy == SortBy.Asc) sortBy(SortBy.Dsc)
        if (sortBy == SortBy.Dsc) sortBy(SortBy.Asc)
    }

    fun groupByOpposite() {
        if (groupBy == GroupBy.Volume) groupBy(GroupBy.Chapter)
        if (groupBy == GroupBy.Chapter) groupBy(GroupBy.Volume)
    }

    fun groupBy(groupBy: GroupBy) {
        onGroupByChange(groupBy)
    }
}

@Composable
fun rememberVolumeItemsState(
    chapters: List<DomainChapter>,
): VolumeItemsState {

    var sortBy by rememberSaveable {
        mutableStateOf(VolumeItemsState.SortBy.Asc)
    }

    var groupBy by rememberSaveable {
        mutableStateOf(VolumeItemsState.GroupBy.Chapter)
    }

    return remember(chapters, sortBy, groupBy) {
        VolumeItemsState(
            chapters, sortBy, groupBy,
            onGroupByChange = {
                groupBy = it
            },
            onSortByChange = {
                sortBy = it
            }
        )
    }
}