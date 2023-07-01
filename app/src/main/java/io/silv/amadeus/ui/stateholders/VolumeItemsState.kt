package io.silv.amadeus.ui.stateholders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.silv.amadeus.domain.models.DomainChapter

class VolumeItemsState(
    private val chapters: List<DomainChapter>,
    private val sortBy: SortBy,
    private val groupBy: GroupBy,
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
        SortBy.Asc -> this.groupBy { it.volume }.values.toList()
        SortBy.Dsc -> this.groupBy { it.volume }.values.reversed()
    }

    val items: VolumeItems by derivedStateOf {
        when(groupBy) {
            GroupBy.Volume -> {
                Volumes(
                    items = chapters.groupByVolumeSorted(sortBy).map { volume ->
                        when (sortBy) {
                            SortBy.Dsc -> volume.sortedByDescending { it.chapter }
                            SortBy.Asc -> volume.sortedBy { it.chapter }
                        }
                    }
                )
            }
            GroupBy.Chapter -> {
                Chapters(
                    items = when(sortBy) {
                        SortBy.Asc -> chapters.sortedBy { it.chapter }
                        SortBy.Dsc -> chapters.sortedByDescending { it.chapter }
                    }
                )
            }
        }
    }

    fun sortBy(sortBy: SortBy) {
        onSortByChange(sortBy)
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