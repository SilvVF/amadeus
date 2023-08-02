@file:OptIn(ExperimentalMaterial3Api::class)

package io.silv.amadeus.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.silv.amadeus.ui.screens.search.SearchItems
import io.silv.amadeus.ui.screens.search.SearchMangaUiState
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.domain.models.DomainManga


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchItems: SearchMangaUiState,
    onBookmarkClick: (manga: DomainManga) -> Unit,
    onMangaClick: (manga: DomainManga) -> Unit
) {
    var searchBarActive by remember {
        mutableStateOf(false)
    }
    val space = LocalSpacing.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = if (searchBarActive)
            Modifier.fillMaxWidth()
        else
            Modifier
                .fillMaxWidth()
                .systemBarsPadding()
    ) {
        if(!searchBarActive) {
            Text(
                text = "Home",
                modifier = Modifier.padding(space.large)
            )
        }
        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onSearch = {},
            modifier = Modifier.weight(1f),
            active = searchBarActive,
            onActiveChange = {
               searchBarActive = it
            },
            placeholder = {
                Text("Quick Search")
            },
            leadingIcon = {
                if (!searchBarActive) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null
                    )
                } else {
                    IconButton(onClick = {
                        searchBarActive = false
                        onSearchQueryChange("")
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            },
            trailingIcon = {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = null
                    )
                }
            }
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()) {
                SearchItems(
                    gridState = rememberLazyGridState(),
                    modifier = Modifier.fillMaxSize(),
                    searchMangaUiState = searchItems,
                    onMangaClick = onMangaClick,
                    onBookmarkClick = onBookmarkClick,
                )
            }
        }
    }
}