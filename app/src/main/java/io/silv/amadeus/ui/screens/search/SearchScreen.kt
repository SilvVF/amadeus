package io.silv.amadeus.ui.screens.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import io.silv.amadeus.AmadeusScaffold
import io.silv.amadeus.ui.screens.home.HomeTab
import io.silv.amadeus.ui.screens.manga_view.MangaViewScreen

class SearchScreen: Screen {

    @OptIn(ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class,
        ExperimentalMaterial3Api::class
    )
    @Composable
    override fun Content() {

        val sm = getScreenModel<SearchSM>()

        val searchMangaPagingItems = sm.searchMangaPagingFlow.collectAsLazyPagingItems()
        val tagsUiState by sm.tagsUiState.collectAsStateWithLifecycle()
        val searchText by sm.searchText.collectAsStateWithLifecycle()
        val includedIds by sm.includedIds.collectAsStateWithLifecycle()
        val excludedIds by sm.excludedIds.collectAsStateWithLifecycle()
        val includedTagsMode by sm.includedTagsMode.collectAsStateWithLifecycle()
        val excludedTagsMode by sm.excludedTagsMode.collectAsStateWithLifecycle()
        val authorQuery by sm.authorQuery.collectAsStateWithLifecycle()
        val artistQuery by sm.artistQuery.collectAsStateWithLifecycle()
        val artistListState by sm.artistListUiState.collectAsStateWithLifecycle()
        val authorListState by sm.authorListUiState.collectAsStateWithLifecycle()
        val selectedAuthors by sm.selectedAuthors.collectAsStateWithLifecycle()
        val selectedArtists by sm.selectedArtists.collectAsStateWithLifecycle()
        val selectedContentRatings by sm.selectedContentRatings.collectAsStateWithLifecycle()
        val selectedStatus by sm.selectedStatus.collectAsStateWithLifecycle()
        val filtering by sm.filtering.collectAsStateWithLifecycle()
        val selectedOriginalLanguages by sm.selectedOrigLangs.collectAsStateWithLifecycle()
        val selectedTranslatedLanguages by sm.selectedTransLang.collectAsStateWithLifecycle()
        val selectedDemographics by sm.selectedDemographics.collectAsStateWithLifecycle()

        val keyboardController = LocalSoftwareKeyboardController.current
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.current

        LaunchedEffect(Unit) {
            var shownOnce = false
            snapshotFlow { filtering }.collect {
                if (shownOnce && !it) { sm.startSearch() }
                if (it) {
                    shownOnce = true
                }
                keyboardController?.hide()
            }
        }

        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
            state = rememberTopAppBarState(),
            snapAnimationSpec = spring()
        )

        AmadeusScaffold(
            scrollBehavior = scrollBehavior,
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            showBottomBar = !filtering,
            topBar = {
                if (!filtering) {
                    TopAppBar(
                        scrollBehavior = scrollBehavior,
                        title = {
                            SearchMangaTopBar(
                                modifier = Modifier.fillMaxWidth(),
                                searchText = searchText,
                                onSearchTextValueChange = {
                                    sm.searchTextChanged(it)
                                },
                                onBackArrowClicked = {
                                    tabNavigator.current = HomeTab
                                },
                                onFilterIconClick = {
                                    sm.isFiltering(true)
                                }
                            )
                        }
                    )
                }
            }
        ) {
                AnimatedContent(
                    targetState = filtering,
                    label = "filter"
                ) { target ->
                    when (target) {
                        true -> FilterScreen(
                            hide = { sm.isFiltering(false) },
                            authorQuery = authorQuery,
                            artistQuery = artistQuery,
                            onArtistQueryChange = sm::artistQueryChanged,
                            onAuthorQueryChange = sm::authorQueryChange,
                            onAuthorSelected = sm::authorSelected,
                            onArtistSelected = sm::artistSelected,
                            authorListState = authorListState,
                            artistListState = artistListState,
                            selectedArtists = selectedArtists,
                            selectedAuthors = selectedAuthors,
                            includedTagsMode = includedTagsMode,
                            excludedTagsMode = excludedTagsMode,
                            includedTagsModeChanged = sm::includedTagModeChange,
                            excludedTagsModeChanged = sm::excludedTagModeChange,
                            tagsUiState = tagsUiState,
                            includedIds = includedIds,
                            excludedIds = excludedIds,
                            includedSelected = sm::includeTagSelected,
                            excludedSelected = sm::excludeTagSelected,
                            contentRatings = sm.contentRatings,
                            onContentRatingSelected = sm::contentRatingSelected,
                            selectedContentRatings = selectedContentRatings,
                            selectedStatus = selectedStatus,
                            statusList = sm.status,
                            onStatusSelected = sm::statusSelected,
                            selectedOriginalLanguages = selectedOriginalLanguages,
                            onOriginalLanguageSelected = sm::selectOriginalLanguage,
                            selectedTranslatedLanguages = selectedTranslatedLanguages,
                            onTranslatedLanguageSelected = sm::selectTranslatedLanguage,
                            selectedDemographics = selectedDemographics,
                            onDemographicSelected = sm::selectDemographic
                        )
                        false -> SearchItemsPagingList(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(it),
                            items = searchMangaPagingItems,
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
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchMangaTopBar(
    modifier: Modifier = Modifier,
    searchText: String,
    onSearchTextValueChange: (String) -> Unit,
    onFilterIconClick: () -> Unit,
    onBackArrowClicked: () -> Unit,
) {
    SearchBar(
        query = searchText,
        onQueryChange = onSearchTextValueChange,
        onSearch = {},
        placeholder = { Text("Search for manga...") },
        modifier = modifier,
        active = false,
        colors = SearchBarDefaults.colors(
            containerColor = Color.Transparent
        ),
        shape = RectangleShape,
        onActiveChange = {},
        leadingIcon = {
            IconButton(onClick = onBackArrowClicked) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = null
                )
            }
        },
        trailingIcon = {
            IconButton(
                onClick = onFilterIconClick
            ) {
                Icon(
                    imageVector = Icons.Outlined.FilterAlt,
                    contentDescription = "filter"
                )
            }
        }
    ){}
}

