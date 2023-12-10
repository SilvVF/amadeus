package io.silv.explore.composables

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.silv.common.model.ContentRating
import io.silv.common.model.QueryResult
import io.silv.common.model.Status
import io.silv.common.model.TagsMode
import io.silv.explore.FilterAction
import io.silv.explore.FilterScreenViewModel
import io.silv.explore.UiQueryFilters
import io.silv.model.DomainAuthor
import io.silv.model.DomainTag
import io.silv.ui.CenterBox
import io.silv.ui.Language
import io.silv.ui.theme.LocalSpacing
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.koinViewModel


@Composable
fun FilterBottomSheetContent(
    hide: () -> Unit,
    onQueryFilterChange: (UiQueryFilters) -> Unit,
    onSaveQueryClick: (UiQueryFilters) -> Unit,
    contentRatingFilters: @Composable ColumnScope.(
        selected: ImmutableList<ContentRating>,
        updateFilter: (FilterAction.ChangeContentRating) -> Unit
    ) -> Unit = { selected, update ->
        DefaultContentRatingsFilter(
            selectedItems = selected,
            updateFilter = update
        )
    },
    statusFilters: @Composable ColumnScope.(
      selected: ImmutableList<Status>,
      updateFilter: (FilterAction.ChangeStatus) -> Unit
    ) -> Unit = { selected, updateFilter ->
        DefaultStatusFilter(
            selectedItems = selected,
            updateFilter = updateFilter
        )
    },
    languagesFilter: @Composable ColumnScope.(
        includedLanguages: ImmutableList<Language>,
        excludedLanguages: ImmutableList<Language>,
        updateFilter: (FilterAction) -> Unit
    ) -> Unit = { includedLanguages, excludedLanguages, updateFilter ->
        DefaultLanguageFilter(
            includedLanguages = includedLanguages,
            excludedLanguages = excludedLanguages,
            updateFilter = updateFilter
        )
    },
    tagsFilter: @Composable ColumnScope.(
        includedTags: ImmutableList<String>,
        excludedTags: ImmutableList<String>,
        categoryToTag: ImmutableMap<String, ImmutableList<DomainTag>>,
        excludeTagsMode: TagsMode,
        includeTagsMode: TagsMode,
        updateFilter: (FilterAction) -> Unit,
    ) -> Unit  = {  includedTags, excludedTags, categoryToTag, includeMode, excludeMode, updateFilter ->
        DefaultTagsFilter(
            includedTags = includedTags,
            excludedTags = excludedTags,
            categoryToTag = categoryToTag,
            includeTagsMode = includeMode,
            excludeTagsMode = excludeMode,
            updateFilter = updateFilter
        )
    },
    hasAvailableChaptersFilter: @Composable ColumnScope.(
       hasAvailableChapters: Boolean,
       updateFilter: (FilterAction.ToggleHasAvailableChapters) -> Unit
    ) -> Unit = {hasAvailableChapters, updateFilter ->
        Row(
            Modifier.clickable {
                updateFilter(FilterAction.ToggleHasAvailableChapters)
            }
        ) {
            Text("Has available chapters")
            Checkbox(
                checked = hasAvailableChapters,
                onCheckedChange = {
                     updateFilter(FilterAction.ToggleHasAvailableChapters)
                }
            )
        }
    }
) {
    val viewModel = koinViewModel<FilterScreenViewModel>()

    BackHandler {
        hide()
    }

    val queryFilterCallback by rememberUpdatedState(newValue = onQueryFilterChange)

    LaunchedEffect(viewModel) {
        viewModel.state.map { it.queryFilters }.collect {
            queryFilterCallback(it)
        }
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val space = LocalSpacing.current
    val scrollState = rememberScrollState()

    val applyButton = @Composable {
        ExtendedFloatingActionButton(
            text = { Text(text = "Apply") },
            containerColor = MaterialTheme.colorScheme.primary,
            icon = { Icon(imageVector = Icons.Filled.Save, contentDescription = null) },
            onClick = { onSaveQueryClick(state.queryFilters) },
            expanded =  true
        )
    }

    Column {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(space.med),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            contentRatingFilters(
                state.queryFilters.contentRating,
                viewModel::updateFilter
            )
            Divider()
            statusFilters(
                state.queryFilters.status,
                viewModel::updateFilter
            )
            Divider()
            tagsFilter(
                state.queryFilters.includedTags,
                state.queryFilters.excludedTags,
                state.categoryToTags,
                state.queryFilters.includedTagsMode,
                state.queryFilters.excludedTagsMode,
                viewModel::updateFilter
            )
            Divider()
            languagesFilter(
                state.queryFilters.originalLanguage,
                state.queryFilters.excludedOriginalLanguage,
                viewModel::updateFilter
            )
            Divider()
            hasAvailableChaptersFilter(
                state.queryFilters.hasAvailableChapters,
                viewModel::updateFilter
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(space.med),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            ExtendedFloatingActionButton(
                text = { Text(text = "Reset") },
                containerColor = MaterialTheme.colorScheme.primary,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null
                    )
                },
                onClick = viewModel::resetFilter,
                expanded =  true
            )
            Spacer(modifier = Modifier.width(space.med))
            applyButton()
        }
    }
}

@Composable
fun DefaultTagsModeFilter(
    tagsMode: TagsMode,
    onToggle: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Tags mode")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = tagsMode == TagsMode.AND,
                onCheckedChange = {
                    onToggle()
                }
            )
            Text("AND")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = tagsMode == TagsMode.OR,
                onCheckedChange = {
                    onToggle()
                }
            )
            Text("OR")
        }
    }
}

@Composable
fun DefaultLanguageFilter(
    includedLanguages: ImmutableList<Language>,
    excludedLanguages: ImmutableList<Language>,
    updateFilter: (FilterAction) -> Unit
) {
    var included by rememberSaveable { mutableStateOf(false) }

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StateFilterChip(
            state = included,
            hideIcons = !included,
            toggleState = { included = !included },
            name = "included languages"
        )
        StateFilterChip(
            state = !included,
            hideIcons = included,
            toggleState = { included = !included },
            name = "excluded languages"
        )
    }
    LanguageSelection(
        selectedLanguages = if (included)
            includedLanguages
        else
            excludedLanguages,
        onLanguageClick = {
            updateFilter(
                if (included) {
                    FilterAction.IncludeLanguage(it)
                }  else {
                    FilterAction.ExcludeLanguage(it)
                }
            )
        }
    )
}

@Composable
fun DefaultTagsFilter(
    includedTags: ImmutableList<String>,
    excludedTags: ImmutableList<String>,
    includeTagsMode: TagsMode,
    excludeTagsMode: TagsMode,
    categoryToTag: ImmutableMap<String, ImmutableList<DomainTag>>,
    updateFilter: (FilterAction) -> Unit,
) {
    var included by rememberSaveable {
        mutableStateOf(true)
    }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StateFilterChip(
            state = included,
            hideIcons = !included,
            toggleState = { included = !included },
            name = "included tags"
        )
        StateFilterChip(
            state = !included,
            hideIcons = included,
            toggleState = { included = !included },
            name ="excluded tags"
        )
    }
    CenterBox(Modifier.fillMaxWidth()) {
        DefaultTagsModeFilter(
            if (included) includeTagsMode else excludeTagsMode,
            onToggle = {
                updateFilter(
                    if (included) {
                        FilterAction.ToggleIncludeTagMode
                    } else {
                        FilterAction.ToggleExcludeTagMode
                    }
                )
            }
        )
    }
    TagsList(
        categoryToTags = categoryToTag,
        selectedTags = if (included) {
            includedTags
        } else excludedTags,
        onTagSelected = {
            if (included) {
                updateFilter(FilterAction.IncludeTag(it))
            } else {
                updateFilter(FilterAction.ExcludeTag(it))
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DefaultStatusFilter(
    selectedItems: ImmutableList<Status>,
    updateFilter: (FilterAction.ChangeStatus) -> Unit,
    item: @Composable (Status) -> Unit = { contentRating ->
        val space = LocalSpacing.current

        val selected by remember(selectedItems) { derivedStateOf { contentRating in selectedItems } }
        var toggleableState by remember(selected) { mutableStateOf(ToggleableState(selected)) }
        var prev by remember { mutableStateOf<ToggleableState?>(null) }

        LaunchedEffect(toggleableState) {
            if (toggleableState == ToggleableState.Indeterminate) {
                prev?.let {
                    delay(3000)
                    toggleableState = it
                }
            } else {
                prev = toggleableState
            }
        }

        TriStateFilterChip(
            state = toggleableState,
            toggleState =  {
                toggleStateIfAble(false, toggleableState) {
                    when (it) {
                        ToggleableState.Off, ToggleableState.On -> updateFilter(FilterAction.ChangeStatus(contentRating))
                        else -> Unit
                    }
                    toggleableState = it
                }
            },
            modifier = Modifier.padding(space.small),
            name = contentRating.toString()
        )
    }
) {
    Column {
        Text(text = "Status")
        FlowRow {

            val statuses = remember { Status.values().toList().toImmutableList() }

            statuses.fastForEach { status ->
                item(status)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DefaultContentRatingsFilter(
    selectedItems: ImmutableList<ContentRating>,
    updateFilter: (FilterAction.ChangeContentRating) -> Unit,
    item: @Composable (ContentRating) -> Unit = { contentRating ->
        val space = LocalSpacing.current

        val selected by remember(selectedItems) { derivedStateOf { contentRating in selectedItems } }
        var toggleableState by remember(selected) { mutableStateOf(ToggleableState(selected)) }
        var prev by remember { mutableStateOf<ToggleableState?>(null) }

        LaunchedEffect(toggleableState) {
            if (toggleableState == ToggleableState.Indeterminate) {
                prev?.let {
                    delay(3000)
                    toggleableState = it
                }
            } else {
                prev = toggleableState
            }
        }

        TriStateFilterChip(
            state = toggleableState,
            toggleState =  {
                toggleStateIfAble(false, toggleableState) {
                    when (it) {
                        ToggleableState.Off, ToggleableState.On -> updateFilter(FilterAction.ChangeContentRating(contentRating))
                        else -> Unit
                    }
                    toggleableState = it
                }
            },
            modifier = Modifier.padding(space.small),
            name = contentRating.toString()
        )
    }
) {
    Column {
        Text(text = "Content Ratings")
        FlowRow {

            val contentRatings = remember { ContentRating.values().toList().toImmutableList() }

            contentRatings.fastForEach { contentRating ->
                item(contentRating)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StateFilterChip(
    state: Boolean,
    toggleState: (Boolean) -> Unit,
    name: String,
    modifier: Modifier = Modifier,
    hideIcons: Boolean = false,
    labelTextStyle: TextStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
) {
    FilterChip(
        modifier = modifier,
        selected = state,
        onClick = { toggleState(!state) },
        leadingIcon = {
            if (!hideIcons) {
                if (state) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                } else  {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                }
            }
        },
        shape = RoundedCornerShape(100),
        label = { Text(text = name, style = labelTextStyle) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            selectedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
            selectedLabelColor = MaterialTheme.colorScheme.primary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            selectedBorderColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelection(
    modifier: Modifier = Modifier,
    selectedLanguages: ImmutableList<Language>,
    onLanguageClick: (Language) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val space = LocalSpacing.current
    val density = LocalDensity.current

    var rowHeightPx by remember {
        mutableIntStateOf(0)
    }

    val languages = remember { Language.values().toList().toImmutableList() }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.onSizeChanged { rowHeightPx = it.height }
        ) {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = null
                )
            }
            if (selectedLanguages.isEmpty()) {
                Text(text = "No language selected")
            } else {
                LazyRow {
                    items(selectedLanguages) {
                        FilterChip(
                            selected = true ,
                            onClick = { onLanguageClick(it) },
                            label = { Text(it.string) },
                            modifier = Modifier.padding(horizontal = space.xs)
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier,
            contentAlignment = Alignment.BottomStart
        ) {
            DropdownMenu(
                expanded = expanded,
                modifier = Modifier.heightIn(0.dp, 200.dp),
                offset = DpOffset(x = 0.dp, y = with(density) { rowHeightPx.toDp() / 2 }),
                onDismissRequest = { expanded = false },
            ) {
                languages.fastForEach { language ->

                    val selected by remember(language, selectedLanguages) {
                        derivedStateOf { language in selectedLanguages }
                    }

                    DropdownMenuItem(
                        onClick = { onLanguageClick(language) },
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
                                checked = selected,
                                onCheckedChange = {
                                    onLanguageClick(language)
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterTopBar(
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
private fun TagsList(
    categoryToTags: ImmutableMap<String, ImmutableList<DomainTag>>,
    selectedTags: ImmutableList<String>,
    onTagSelected: (id: String) -> Unit
) {

    val space = LocalSpacing.current

    Column(
        Modifier
            .padding(space.med)
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        for((group, tags) in categoryToTags) {

            var expanded by rememberSaveable { mutableStateOf(false) }

            val categorySelected by remember(tags, selectedTags) {
                derivedStateOf { tags.filter { it.id in selectedTags } }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(space.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = group,
                    style = MaterialTheme.typography.labelLarge
                )
                IconButton(
                    modifier = Modifier.padding(horizontal = space.small),
                    onClick = { expanded = !expanded }
                ) {
                    Icon(
                        imageVector = if (!expanded) {
                            Icons.Filled.ArrowDropDown
                        } else {
                            Icons.Filled.ArrowDropUp
                        },
                        contentDescription = null
                    )
                }
                AnimatedVisibility(!expanded) {
                    LazyRow(Modifier.weight(1f)) {
                        items(categorySelected) { tag ->
                            FilterChip(
                                selected = true,
                                onClick = { onTagSelected(tag.id) },
                                label = { Text(tag.name) },
                                modifier = Modifier.padding(horizontal = space.xs)
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FlowRow {
                    tags.fastForEach { tag ->
                        FilterChip(
                            selected = tag.id in selectedTags,
                            onClick = { onTagSelected(tag.id) },
                            label = { Text(tag.name) },
                            modifier = Modifier.padding(horizontal = space.xs)
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun AuthorTextField(
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
private fun AuthorSearchBar(
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
                                        .padding(space.med)
                                ) {
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriStateFilterChip(
    state: ToggleableState,
    toggleState: (ToggleableState) -> Unit,
    name: String,
    modifier: Modifier = Modifier,
    hideIcons: Boolean = false,
    labelTextStyle: TextStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
) {
    FilterChip(
        modifier = modifier,
        selected = state == ToggleableState.On || state == ToggleableState.Indeterminate,
        onClick = { toggleStateIfAble(false, state, toggleState) },
        leadingIcon = {
            if (!hideIcons) {
                if (state == ToggleableState.On) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                } else if (state == ToggleableState.Indeterminate) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                }
            }
        },
        shape = RoundedCornerShape(100),
        label = { Text(text = name, style = labelTextStyle) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            selectedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
            selectedLabelColor = MaterialTheme.colorScheme.primary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            selectedBorderColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
        ),
    )
}

private fun toggleStateIfAble(disabled: Boolean, state: ToggleableState, toggleState: (ToggleableState) -> Unit) {
    if (!disabled) {
        val newState = when (state) {
            ToggleableState.On -> ToggleableState.Indeterminate
            ToggleableState.Indeterminate -> ToggleableState.Off
            ToggleableState.Off -> ToggleableState.On
        }
        toggleState(newState)
    }
}