package io.silv.amadeus.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import io.silv.amadeus.types.SavableManga
import io.silv.amadeus.ui.composables.AnimatedBoxShimmer
import io.silv.amadeus.ui.screens.manga_filter.MangaFilterScreen
import io.silv.amadeus.ui.screens.manga_view.MangaViewScreen
import io.silv.amadeus.ui.shared.CenterBox
import io.silv.amadeus.ui.theme.LocalSpacing

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.seasonalMangaLists(
    refreshingSeasonal: Boolean,
    seasonalMangaState: SeasonalMangaUiState,
    onBookmarkClick: (manga: SavableManga) -> Unit,
) {
    item {

        val space = LocalSpacing.current
        val navigator = LocalNavigator.current
        var selectedIndex by rememberSaveable {
            mutableStateOf(0)
        }

        Column(Modifier.padding(space.med)) {
            Text(
                "seasonal lists",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(space.xs)
            )
            if (refreshingSeasonal) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(4) {
                        AnimatedBoxShimmer(
                            Modifier
                                .weight(1f)
                                .height(40.dp))
                    }
                }
                AnimatedBoxShimmer(
                    Modifier
                        .height(240.dp)
                        .fillMaxWidth()
                )
            } else {
                if (seasonalMangaState.seasonalLists.isEmpty()) {
                    CenterBox(Modifier.size(200.dp).padding(space.med)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "unable to load, make sure to check your network connection.")
                        }
                    }
                } else {
                    LazyRow {
                        itemsIndexed(
                            seasonalMangaState.seasonalLists,
                            key = { _, list -> list.id }
                        ) { index, seasonalList ->
                            FilterChip(
                                selected = index == selectedIndex,
                                onClick = { selectedIndex = index },
                                label = {
                                    Text(
                                        "${seasonalList.season.name}  ${seasonalList.year.toString().takeLast(2)}",
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(space.xs)
                            )
                        }
                    }
                    MangaPager(
                        mangaList = seasonalMangaState.seasonalLists.getOrNull(selectedIndex)?.mangas ?: emptyList(),
                        onTagClick = { name , id ->
                            navigator?.push(
                                MangaFilterScreen(name, id)
                            )
                        },
                        onMangaClick = {
                            navigator?.push(
                                MangaViewScreen(it)
                            )
                        },
                        onBookmarkClick = {
                            onBookmarkClick(it)
                        },
                    )
                }
            }
        }
    }
}
