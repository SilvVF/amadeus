package io.silv.model

import androidx.compose.runtime.Stable
import io.silv.common.model.Season
import io.silv.domain.manga.model.Manga

@Stable
data class DomainSeasonalList(
    val id: String,
    val season: Season,
    val year: Int,
    val mangas: List<Manga>,
)
