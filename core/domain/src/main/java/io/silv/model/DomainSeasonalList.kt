package io.silv.model

import androidx.compose.runtime.Stable
import io.silv.common.model.Season
import io.silv.domain.manga.model.Manga
import kotlinx.collections.immutable.ImmutableList

@Stable
data class DomainSeasonalList(
    val id: String,
    val season: Season,
    val year: Int,
    val mangas: ImmutableList<Manga>,
)
