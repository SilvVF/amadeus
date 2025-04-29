package io.silv.explore

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import io.silv.common.model.Season
import io.silv.data.manga.model.Manga

@Stable
@Immutable
data class SeasonalMangaUiState(
    val seasonalLists: List<SeasonalList> = emptyList(),
) {
    @Stable
    @Immutable
    data class SeasonalList(
        val id: String,
        val year: Int,
        val season: Season,
        val mangas: List<Manga>,
    )
}
