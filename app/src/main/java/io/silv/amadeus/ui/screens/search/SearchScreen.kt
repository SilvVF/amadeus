package io.silv.amadeus.ui.screens.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import com.skydoves.orbital.Orbital
import com.skydoves.orbital.animateMovement
import com.skydoves.orbital.rememberContentWithOrbitalScope
import io.silv.amadeus.ui.composables.AnimatedBoxShimmer
import io.silv.amadeus.ui.composables.MangaListItem
import io.silv.amadeus.ui.screens.home.HomeTab
import io.silv.amadeus.ui.screens.manga_filter.MangaFilterScreen
import io.silv.amadeus.ui.screens.manga_view.MangaViewScreen
import io.silv.amadeus.ui.shared.CenterBox
import io.silv.amadeus.ui.shared.Language
import io.silv.amadeus.ui.shared.noRippleClickable
import io.silv.amadeus.ui.theme.LocalBottomBarVisibility
import io.silv.amadeus.ui.theme.LocalPaddingValues
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.domain.models.DomainAuthor
import io.silv.manga.domain.models.DomainManga
import io.silv.manga.domain.models.DomainTag
import io.silv.manga.domain.repositorys.people.QueryResult
import io.silv.manga.network.mangadex.models.ContentRating
import io.silv.manga.network.mangadex.models.PublicationDemographic
import io.silv.manga.network.mangadex.models.Status
import io.silv.manga.network.mangadex.requests.MangaRequest

class SearchScreen: Screen {

    @OptIn(ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class)
    @Composable
    override fun Content() {

        val sm = getScreenModel<SearchSM>()

        val searchMangaUiState by sm.searchMangaUiState.collectAsStateWithLifecycle()
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

        val lazyGridState = rememberLazyGridState()
        val keyboardController = LocalSoftwareKeyboardController.current
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.current
        var bottomBarVisibility by LocalBottomBarVisibility.current
        val topLevelPadding by LocalPaddingValues.current

        LaunchedEffect(Unit) {
            sm.isFiltering(false)
        }

        LaunchedEffect(Unit) {
            var shownOnce = false
            snapshotFlow { filtering }.collect {
                bottomBarVisibility = !it
                if (shownOnce && !it) { sm.startSearch() }
                if (it) {
                    shownOnce = true
                }
                keyboardController?.hide()
            }
        }

        LaunchedEffect(lazyGridState) {
            snapshotFlow { lazyGridState.firstVisibleItemIndex }.collect { idx ->
                (searchMangaUiState as? SearchMangaUiState.Success)?.let {
                    if (idx >= it.results.lastIndex - 6) {
                        sm.loadNextSearchPage()
                    }
                }
            }
        }

        Scaffold(
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.systemBars)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
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
                        false -> Column(
                            Modifier.systemBarsPadding()
                        ) {
                            SearchMangaTopBar(
                                searchText = searchText,
                                onSearchTextValueChange = {
                                    sm.searchTextChanged(it)
                                },
                                onBackArrowClicked = {
                                    tabNavigator.current = HomeTab
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
                                },
                                onFilterIconClick = {
                                    sm.isFiltering(true)
                                }
                            )
                            SearchItems(
                                modifier = Modifier
                                    .padding(topLevelPadding)
                                    .padding(it)
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
        }
    }
}


@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun FilterScreen(
    hide: () -> Unit,
    contentRatings: List<ContentRating>,
    selectedContentRatings: List<ContentRating>,
    onContentRatingSelected: (ContentRating) -> Unit,
    statusList: List<Status>,
    selectedStatus: List<Status>,
    onStatusSelected: (Status) -> Unit,
    authorQuery: String,
    artistQuery: String,
    onArtistQueryChange: (query: String) -> Unit,
    onAuthorQueryChange: (query: String) -> Unit,
    onAuthorSelected: (author: DomainAuthor) -> Unit,
    onArtistSelected: (author: DomainAuthor) -> Unit,
    authorListState: QueryResult<List<DomainAuthor>>,
    artistListState: QueryResult<List<DomainAuthor>>,
    selectedArtists: List<DomainAuthor>,
    selectedAuthors: List<DomainAuthor>,
    includedTagsMode: MangaRequest.TagsMode,
    excludedTagsMode: MangaRequest.TagsMode,
    includedTagsModeChanged: (MangaRequest.TagsMode) -> Unit,
    excludedTagsModeChanged: (MangaRequest.TagsMode) -> Unit,
    tagsUiState: List<DomainTag>,
    includedIds: List<String>,
    excludedIds: List<String>,
    includedSelected: (String) -> Unit,
    excludedSelected: (String) -> Unit,
    selectedOriginalLanguages: List<Language>,
    onOriginalLanguageSelected: (Language) -> Unit,
    selectedTranslatedLanguages: List<Language>,
    onTranslatedLanguageSelected: (Language) -> Unit,
    selectedDemographics: List<PublicationDemographic>,
    onDemographicSelected: (PublicationDemographic) -> Unit
) {
    val space = LocalSpacing.current
    val groupedTags = remember(tagsUiState) {
        tagsUiState.groupBy { it.group }
    }
    var included by rememberSaveable {
        mutableStateOf(true)
    }
    var tagsVisible by rememberSaveable {
        mutableStateOf(true)
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .systemBarsPadding()
    ) {
        FilterTopBar {
            hide()
        }
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(bottom = space.large)
        ) {
            item {
                Column {
                    Row(Modifier.fillMaxWidth()) {
                        AuthorSearchBar(
                            modifier = Modifier
                                .padding(space.small)
                                .weight(1f),
                            label = "Authors",
                            query = authorQuery,
                            onQueryChange = onAuthorQueryChange,
                            result = authorListState,
                            onAuthorSelected = onAuthorSelected,
                            selectedAuthors = selectedAuthors
                        )
                        AuthorSearchBar(
                            modifier = Modifier
                                .padding(space.small)
                                .weight(1f),
                            label = "Artists",
                            query = artistQuery,
                            addCords = true,
                            onQueryChange = onArtistQueryChange,
                            result = artistListState,
                            onAuthorSelected = onArtistSelected,
                            selectedAuthors = selectedArtists,
                        )
                    }
                    Spacer(Modifier.height(space.med))
                }
            }
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = space.med)) {
                    Text("Content Rating")
                    FlowRow {
                        contentRatings.forEach { rating ->
                            FilterChip(
                                selected = remember(contentRatings, selectedContentRatings) {
                                    selectedContentRatings.contains(rating)
                                },
                                onClick = { onContentRatingSelected(rating) },
                                label = { Text(rating.name) },
                                modifier = Modifier.padding(horizontal = space.xs)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = space.med)) {
                    Text("Publication Status")
                    FlowRow {
                        statusList.forEach { status ->
                            FilterChip(
                                selected = remember(status, selectedStatus) {
                                    selectedStatus.contains(status)
                                },
                                onClick = { onStatusSelected(status) },
                                label = { Text(status.name) },
                                modifier = Modifier.padding(horizontal = space.xs)
                            )
                        }
                    }
                    Spacer(Modifier.height(space.med))
                }
            }
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = space.med)) {
                    val demographics = remember {
                        PublicationDemographic.values().toList()
                    }
                    Text("Magazine Demographic")
                    FlowRow {
                        demographics.forEach { demographic ->
                            FilterChip(
                                selected = remember(demographic, selectedDemographics) {
                                    selectedDemographics.contains(demographic)
                                },
                                onClick = { onDemographicSelected(demographic) },
                                label = { Text(demographic.name) },
                                modifier = Modifier.padding(horizontal = space.xs)
                            )
                        }
                    }
                    Spacer(Modifier.height(space.med))
                }
            }
            item {
                Column(Modifier.padding(horizontal = space.med)) {
                    LanguageSelection(
                        label = {
                            Text("Original Language")
                        },
                        placeholder = "All Languages",
                        selected = selectedOriginalLanguages,
                        onLanguageSelected = onOriginalLanguageSelected,
                    )
                    Spacer(Modifier.height(space.med))
                    LanguageSelection(
                        label = {
                            Text("Translated Languages")
                        },
                        placeholder = "All Languages",
                        selected = selectedTranslatedLanguages,
                        onLanguageSelected = onTranslatedLanguageSelected,
                    )
                    Spacer(Modifier.height(space.med))
                }
            }
            stickyHeader {
               TagsFilterHeader(
                   includedTagsMode = includedTagsMode,
                   includedTagsModeChanged = includedTagsModeChanged,
                   excludedTagsMode = excludedTagsMode,
                   excludedTagsModeChanged = excludedTagsModeChanged,
                   tagsVisible = tagsVisible,
                   tagsVisibleChange = { tagsVisible = it },
                   includedChange = { included = it },
                   included = included
               )
            }
            item {
                TagsList(
                    tagsVisible = tagsVisible,
                    included = included,
                    groupedTags = groupedTags,
                    includedIds = includedIds,
                    includedSelected = includedSelected,
                    excludedIds = excludedIds,
                    excludedSelected = excludedSelected
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelection(
    label: (@Composable () -> Unit)? = null,
    placeholder: String,
    selected: List<Language>,
    onLanguageSelected: (Language) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val maxHeight = remember(screenHeightDp) {
        screenHeightDp / 3f
    }
    val space = LocalSpacing.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.BottomStart)
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            label?.invoke()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = null
                    )
                }
                if (selected.isEmpty()) {
                    Text(text = placeholder)
                } else {
                    when (selected.size) {
                        in 1..2 -> {
                            selected.forEach {
                                FilterChip(
                                    selected = true ,
                                    onClick = { onLanguageSelected(it) },
                                    label = { Text(it.string) },
                                    modifier = Modifier.padding(horizontal = space.xs)
                                )
                            }
                        }
                        else -> {
                            FilterChip(
                                selected = true ,
                                onClick = { onLanguageSelected(selected.first()) },
                                label = { Text(selected.first().string) },
                                modifier = Modifier.padding(horizontal = space.xs)
                            )
                            AssistChip(
                                onClick = { expanded = !expanded },
                                enabled = true,
                                label = { Text("+${selected.size - 1} more") },
                                modifier = Modifier.padding(horizontal = space.xs)
                            )
                        }
                    }
                }
            }
        }
        DropdownMenu(
            expanded = expanded,
            modifier = Modifier.heightIn(0.dp, maxHeight),
            onDismissRequest = { expanded = false },
        ) {
           Language.values().forEach { language ->
                DropdownMenuItem(
                    onClick = { onLanguageSelected(language) },
                    text = { Text(language.string) },
                    leadingIcon = {
                        Image(
                            painter = painterResource(id = language.resId),
                            contentDescription = "flag",
                            modifier = Modifier.size(50.dp),
                            contentScale = ContentScale.Fit
                        )
                    },
                    trailingIcon = {
                        Checkbox(
                            checked = remember(language, selected) {
                                derivedStateOf { language in selected }.value
                            },
                            onCheckedChange = {
                                onLanguageSelected(language)
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun FilterTopBar(
    onCloseIconClick: () -> Unit
) {
    val space = LocalSpacing.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(space.med),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "Filters", style = MaterialTheme.typography.titleLarge)
        IconButton(onClick = onCloseIconClick) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TagsList(
    tagsVisible: Boolean,
    included: Boolean,
    groupedTags: Map<String, List<DomainTag>>,
    includedIds: List<String>,
    includedSelected: (String) -> Unit,
    excludedIds: List<String>,
    excludedSelected: (String) -> Unit
) {
    val space = LocalSpacing.current
    Column {
        AnimatedVisibility(
            visible = tagsVisible
        ) {
            Column {
                if (included) {
                    groupedTags.forEach { (group, tags) ->
                        Text(
                            text = group,
                            style = MaterialTheme.typography.labelLarge
                        )
                        FlowRow {
                            tags.forEach {
                                FilterChip(
                                    selected = it.id in includedIds,
                                    onClick = { includedSelected(it.id) },
                                    label = { Text(it.name) },
                                    modifier = Modifier.padding(horizontal = space.xs)
                                )
                            }
                        }
                    }
                } else {
                    groupedTags.forEach { (group, tags) ->
                        Text(
                            text = group,
                            style = MaterialTheme.typography.labelLarge
                        )
                        FlowRow {
                            tags.forEach {
                                FilterChip(
                                    selected = it.id in excludedIds,
                                    onClick = { excludedSelected(it.id) },
                                    label = { Text(it.name) },
                                    modifier = Modifier.padding(horizontal = space.xs)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TagsFilterHeader(
    includedTagsMode: MangaRequest.TagsMode,
    includedTagsModeChanged: (MangaRequest.TagsMode) -> Unit,
    excludedTagsMode: MangaRequest.TagsMode,
    excludedTagsModeChanged: (MangaRequest.TagsMode) -> Unit,
    tagsVisible: Boolean,
    tagsVisibleChange: (Boolean) -> Unit,
    includedChange: (Boolean) -> Unit,
    included: Boolean,
) {
    val space = LocalSpacing.current
    Surface {
        Column {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TagModeSwitch(
                    title = "Tag inclusion mode",
                    mode = includedTagsMode,
                    onModeChange = {
                        includedTagsModeChanged(it)
                    },
                    modifier = Modifier.weight(1f)
                )
                TagModeSwitch(
                    title = "Tag exclusion mode",
                    mode = excludedTagsMode,
                    onModeChange = {
                        excludedTagsModeChanged(it)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = tagsVisible,
                    onCheckedChange = {
                        tagsVisibleChange(it)
                    }
                )
                Text("show tags")
                IconButton(onClick = { tagsVisibleChange(!tagsVisible) }) {
                    Icon(
                        imageVector = if (tagsVisible) {
                            Icons.Filled.KeyboardArrowUp
                        } else
                            Icons.Filled.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            }
            TagSelectRow(
                modifier = Modifier
                    .padding(vertical = space.med)
                    .fillMaxWidth()
                    .height(40.dp),
                included = included,
                onIncludedChange = includedChange
            )
        }
    }
}

@Composable
fun AuthorTextField(
    modifier: Modifier = Modifier,
    value: String,
    labelString: String,
    onValueChanged: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChanged,
        modifier = modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyMedium,
        label = {
            Text(
                text = labelString,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp
            )
        },
        singleLine = true,
        leadingIcon = {
            Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChanged("") }) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = null
                    )
                }
            }
        }
    )

}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun AuthorSearchBar(
    modifier: Modifier = Modifier,
    label: String,
    query: String,
    onQueryChange: (query: String) -> Unit,
    result: QueryResult<List<DomainAuthor>>,
    selectedAuthors: List<DomainAuthor>,
    addCords: Boolean = false,
    onAuthorSelected: (author: DomainAuthor) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val space = LocalSpacing.current
    val focusManager = LocalFocusManager.current
    var currentCoordinates: IntOffset by remember { mutableStateOf(IntOffset(0, 0)) }
    var sizeOffset: IntSize by remember { mutableStateOf(IntSize(0, 0)) }

    val popupPositionProvider = object : PopupPositionProvider {
        override fun calculatePosition(
            anchorBounds: IntRect,
            windowSize: IntSize,
            layoutDirection: LayoutDirection,
            popupContentSize: IntSize
        ): IntOffset {
            return currentCoordinates.copy(
                x = currentCoordinates.x + if (addCords) sizeOffset.width else 0,
                y = currentCoordinates.y + sizeOffset.height
            )
        }
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Bottom
    ) {
        LaunchedEffect(Unit) {
            snapshotFlow { currentCoordinates.y }.collect {
               if((it > (0 + sizeOffset.height)) && focused) {
                   keyboardController?.hide()
                   focusManager.clearFocus(true)
               }
            }
        }

        AuthorTextField(
            value = query,
            labelString = label,
            onValueChanged = onQueryChange,
            modifier = Modifier
                .onGloballyPositioned { layoutCoordinates ->
                    val (x: Int, y: Int) = when {
                        layoutCoordinates.isAttached -> with(layoutCoordinates.positionInRoot()) {
                            x.toInt() to y.toInt()
                        }

                        else -> 0 to 0
                    }
                    currentCoordinates = IntOffset(x, y)
                    sizeOffset = layoutCoordinates.size
                }
                .focusRequester(focusRequester)
                .onFocusChanged {
                    focused = it.isFocused
                }
        )
        LazyRow {
            items(selectedAuthors) {
                FilterChip(
                    selected = true,
                    onClick = { onAuthorSelected(it) },
                    label = { Text(it.name) },
                    modifier = Modifier.padding(horizontal = space.xs)
                )
            }
        }
        val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
        AnimatedVisibility(
            visible = query.isNotEmpty() && (currentCoordinates.y > 0 + sizeOffset.height),
        ) {
            Popup(
                popupPositionProvider = popupPositionProvider,
                properties = PopupProperties()
            ) {
                LazyColumn(
                    Modifier
                        .fillMaxWidth(0.5f)
                        .heightIn(0.dp, screenHeightDp / 3)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    when (result) {
                        is QueryResult.Done -> {
                            if (result.result.isEmpty() && query.isNotEmpty()) {
                                item {
                                    Text(text = "No $label found")
                                }
                            }
                            items(result.result) {
                                DropdownMenuItem(
                                    text = { Text(it.name) },
                                    trailingIcon = {
                                        if (it in selectedAuthors) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    onClick = { onAuthorSelected(it) }
                                )
                                Divider()
                            }
                        }
                        QueryResult.Loading ->  {
                            item {
                                CenterBox(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(space.med)) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TagModeSwitch(
    modifier: Modifier = Modifier,
    title: String,
    mode: MangaRequest.TagsMode = MangaRequest.TagsMode.AND,
    icon: (@Composable () -> Unit)? = null,
    onModeChange: (MangaRequest.TagsMode) -> Unit,
) {
    val space = LocalSpacing.current
    val transformationSpec = SpringSpec<IntOffset>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = 4000f
    )
    val transformed = remember(mode) { mode == MangaRequest.TagsMode.OR }
    val switch = rememberContentWithOrbitalScope {
        CenterBox(Modifier.animateMovement(this@rememberContentWithOrbitalScope, transformationSpec)) {
            CenterBox(
                modifier = Modifier
                    .clip(CircleShape)
                    .size(22.dp)
                    .background(MaterialTheme.colorScheme.primary),
            ) {
                icon?.invoke()
            }
        }
    }
    Column(modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "AND",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = if (transformed) {
                        Color.Unspecified
                    } else  MaterialTheme.colorScheme.primary,
                    fontWeight = if (transformed) {
                        FontWeight.Normal
                    } else  FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = space.med)
            )
            Orbital(
                Modifier
                    .width(60.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(100))
                    .clickable {
                        onModeChange(
                            if (mode == MangaRequest.TagsMode.OR)
                                MangaRequest.TagsMode.AND
                            else
                                MangaRequest.TagsMode.OR
                        )
                    }
            ) {
                if (!transformed) {
                    Row(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(100))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                3.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(100)
                            )
                            .padding(space.small),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        switch()
                    }
                } else {
                    Row(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(100))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                3.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(100)
                            )
                            .padding(space.small),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        switch()
                    }
                }
            }
            Text(
                text = "OR",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = if (!transformed) {
                        Color.Unspecified
                    } else  MaterialTheme.colorScheme.primary,
                    fontWeight = if (!transformed) {
                            FontWeight.Normal
                        } else  FontWeight.Bold
                ),
                modifier =  Modifier.padding(horizontal = space.med)
            )
        }
    }
}

@Composable
fun TagSelectRow(
    modifier: Modifier = Modifier,
    included: Boolean,
    onIncludedChange: (Boolean) -> Unit
) {
    val movementSpec = SpringSpec<IntOffset>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = 200f
    )

    val backgroundBox = rememberContentWithOrbitalScope {
        Box(
            Modifier
                .fillMaxWidth(0.5f)
                .animateMovement(
                    this@rememberContentWithOrbitalScope,
                    movementSpec
                )
        ) {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
            )
        }

    }

    Column(modifier) {
        Orbital(
            Modifier
                .noRippleClickable { onIncludedChange(!included) }
                .fillMaxWidth()
        ) {
            if (included) {
                Box(Modifier
                    .fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    backgroundBox()
                    TextItems()
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    backgroundBox()
                    TextItems()
                }
            }
        }
    }
}

@Composable
private fun TextItems() {
    Row(Modifier
        .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        listOf("Included", "Excluded").forEach {
            Text(
                text = it,
                modifier = Modifier
                    .weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            )
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
    onFilterIconClick: () -> Unit,
    onBackArrowClicked: () -> Unit,
) {
    val selectedExcluded = remember(excludedTags, selectedExcludedIds) {
        excludedTags.filter { it.id in selectedExcludedIds }
    }

    val selectedIncluded = remember(includedTags, selectedIncludedIds) {
        includedTags.filter { it.id in selectedIncludedIds }
    }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        SearchBar(
            query = searchText,
            onQueryChange = onSearchTextValueChange,
            onSearch = {},
            placeholder = {
                          Text("Search for manga...")
            },
            active = false,
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
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            if (selectedIncluded.isNotEmpty()) {
                Text("included tags", style = MaterialTheme.typography.labelSmall)
            }
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
            if (selectedExcluded.isNotEmpty()) {
                Text("excluded tags", style = MaterialTheme.typography.labelSmall)
            }
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
fun SearchItems(
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