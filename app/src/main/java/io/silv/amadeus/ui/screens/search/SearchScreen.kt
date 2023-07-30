package io.silv.amadeus.ui.screens.search

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import io.silv.amadeus.ui.composables.AnimatedBoxShimmer
import io.silv.amadeus.ui.composables.MangaListItem
import io.silv.amadeus.ui.screens.manga_filter.MangaFilterScreen
import io.silv.amadeus.ui.screens.manga_view.MangaViewScreen
import io.silv.amadeus.ui.shared.CenterBox
import io.silv.amadeus.ui.theme.LocalPaddingValues
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.domain.models.DomainManga
import io.silv.manga.domain.models.DomainTag

class SearchScreen: Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {

        val sm = getScreenModel<SearchSM>()

        val searchMangaUiState by sm.searchMangaUiState.collectAsStateWithLifecycle()
        val tagsUiState by sm.tagsUiState.collectAsStateWithLifecycle()
        val searchText by sm.searchText.collectAsStateWithLifecycle()
        val includedIds by sm.includedIds.collectAsStateWithLifecycle()
        val excludedIds by sm.excludedIds.collectAsStateWithLifecycle()

        val scope = rememberCoroutineScope()
        val bottomSheetState = rememberBottomSheetScaffoldState(
            bottomSheetState = SheetState(skipPartiallyExpanded = true, initialValue = SheetValue.Hidden)
        )

        val lazyGridState = rememberLazyGridState()

        val space = LocalSpacing.current
        val navigator = LocalNavigator.current
        val topLevelPadding by LocalPaddingValues.current

        LaunchedEffect(lazyGridState) {
            snapshotFlow { lazyGridState.firstVisibleItemIndex }.collect { idx ->
                (searchMangaUiState as? SearchMangaUiState.Success)?.let {
                    if (idx >= it.results.lastIndex - 6) {
                        Log.d("search", "searching")
                        sm.loadNextSearchPage()
                    }
                }
            }
        }

        BottomSheetScaffold(
            sheetContent = {},
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                Modifier
                    .padding(paddingValues)
                    .systemBarsPadding()
            ) {
                SearchMangaTopBar(
                    searchText = searchText,
                    onSearchTextValueChange = {
                        sm.searchTextChanged(it)
                    },
                    onBackArrowClicked = {
                        navigator?.pop()
                    },
                    includedTags = tagsUiState,
                    excludedTags = tagsUiState,
                    selectedIncludedIds = includedIds,
                    selectedExcludedIds = excludedIds,
                    onExcludedTagSelected =  {
                        sm.excludeTagSelected(it)
                    },
                    onIncludedTagSelected = {
                        sm.includeTagSelected(it)
                    }
                )
                SearchItems(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    searchMangaUiState = searchMangaUiState,
                    gridState = lazyGridState,
                    onMangaClick = {
                         navigator?.push(
                             MangaViewScreen(it)
                         )
                    },
                    onBookmarkClick = {
                        sm.bookmarkManga(it.id)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchMangaTopBar(
    searchText: String,
    includedTags: List<DomainTag>,
    selectedIncludedIds: List<String>,
    excludedTags: List<DomainTag>,
    selectedExcludedIds: List<String>,
    onIncludedTagSelected: (id: String) -> Unit,
    onExcludedTagSelected: (id: String) -> Unit,
    onSearchTextValueChange: (String) -> Unit,
    onBackArrowClicked: () -> Unit,
) {
    val space = LocalSpacing.current

    val selectedExcluded = remember(includedTags, selectedIncludedIds) {
        includedTags.filter { it.id in selectedIncludedIds }
    }

    val selectedIncluded = remember(includedTags, selectedExcludedIds) {
        excludedTags.filter { it.id in selectedExcludedIds }
    }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            IconButton(onClick = onBackArrowClicked) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = null
                )
            }
            var focused by remember { mutableStateOf(false) }
            val bgc = MaterialTheme.colorScheme.onBackground
            BasicTextField(
                value = searchText,
                modifier = Modifier.onFocusChanged {
                    focused = it.isFocused
                },
                singleLine = true,
                textStyle = TextStyle(
                    color = bgc,
                    fontSize = 24.sp
                ),
                cursorBrush = SolidColor(bgc),
                onValueChange = onSearchTextValueChange,
                decorationBox = { innerTextField ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(60.dp)
                            .padding(space.small)
                            .drawBehind {
                                drawLine(
                                    color = bgc,
                                    start = Offset(0f, this.size.height),
                                    end = Offset(this.size.width, this.size.height),
                                    strokeWidth = 2f
                                )
                            }
                    ) {
                        if (searchText.isEmpty() && !focused) {
                            Text("Search for manga...", color = bgc, fontSize = 18.sp)
                        }
                        innerTextField()
                    }
                }
            )
        }
        Text("included tags", style = MaterialTheme.typography.labelSmall)
        LazyRow {
            items(
                items = selectedIncluded,
                key = { tag -> tag.id }
            ) {tag ->
                FilterChip(
                    onClick = { onIncludedTagSelected(tag.id) },
                    label = { Text(text = tag.name) },
                    selected = true
                )
            }
        }
        Text("excluded tags", style = MaterialTheme.typography.labelSmall)
        LazyRow {
            items(
                items = selectedExcluded,
                key = { tag -> tag.id }
            ) {tag ->
                FilterChip(
                    onClick = { onExcludedTagSelected(tag.id) },
                    label = { Text(text = tag.name) },
                    selected = true
                )
            }
        }
    }
}


@Composable
fun SearchItemsList(
    modifier: Modifier = Modifier,
    items: List<DomainManga>,
    state: LazyGridState = rememberLazyGridState(),
    onMangaClick: (manga: DomainManga) -> Unit,
    onBookmarkClick: (manga: DomainManga) -> Unit
) {
    val space = LocalSpacing.current
    val navigator = LocalNavigator.current

    LazyVerticalGrid(
        modifier = modifier,
        state = state,
        columns = GridCells.Fixed(2)
    ) {
        items(
            items = items,
            key = { item: DomainManga -> item.id }
        ) { manga ->
            MangaListItem(
                manga = manga,
                modifier = Modifier
                    .padding(space.large)
                    .clickable {
                        onMangaClick(manga)
                    },
                onBookmarkClick = {
                    onBookmarkClick(manga)
                },
                onTagClick = { name ->
                    manga.tagToId[name]?.let {
                        navigator?.push(
                            MangaFilterScreen(name, it)
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun SearchItems(
    modifier: Modifier,
    searchMangaUiState: SearchMangaUiState,
    gridState: LazyGridState,
    onMangaClick: (DomainManga) -> Unit,
    onBookmarkClick: (DomainManga) -> Unit
) {
    when(searchMangaUiState) {
        is SearchMangaUiState.Refreshing -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
            ) {
                items(4) {
                    AnimatedBoxShimmer(Modifier.size(300.dp))
                }
            }
        }
        is SearchMangaUiState.Success -> {
            SearchItemsList(
                state = gridState,
                modifier = modifier,
                items = searchMangaUiState.results,
                onMangaClick = onMangaClick,
                onBookmarkClick = onBookmarkClick
            )
        }
        is SearchMangaUiState.WaitingForQuery -> {
            CenterBox(modifier = Modifier.fillMaxSize()) {
                Text("use filters to search for manga")
            }
        }
    }
}