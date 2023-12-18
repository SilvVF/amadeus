package io.silv.model

import androidx.compose.runtime.Stable
import io.silv.common.model.Season
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow

@Stable
data class DomainSeasonalList(
    val id: String,
    val season: Season,
    val year: Int,
    val mangas: ImmutableList<StateFlow<SavableManga>>,
)
