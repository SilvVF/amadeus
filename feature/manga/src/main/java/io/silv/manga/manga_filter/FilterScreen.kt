package io.silv.manga.manga_filter

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Clear
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
import io.silv.common.model.ContentRating
import io.silv.common.model.PublicationDemographic
import io.silv.common.model.QueryResult
import io.silv.common.model.Status
import io.silv.common.model.TagsMode
import io.silv.model.DomainAuthor
import io.silv.model.DomainTag
import io.silv.ui.CenterBox
import io.silv.ui.noRippleClickable
import io.silv.ui.theme.LocalSpacing

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
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
    includedTagsMode: TagsMode,
    excludedTagsMode: TagsMode,
    includedTagsModeChanged: (TagsMode) -> Unit,
    excludedTagsModeChanged: (TagsMode) -> Unit,
    tagsUiState: List<DomainTag>,
    includedIds: List<String>,
    excludedIds: List<String>,
    includedSelected: (String) -> Unit,
    excludedSelected: (String) -> Unit,
    selectedOriginalLanguages: List<io.silv.ui.Language>,
    onOriginalLanguageSelected: (io.silv.ui.Language) -> Unit,
    selectedTranslatedLanguages: List<io.silv.ui.Language>,
    onTranslatedLanguageSelected: (io.silv.ui.Language) -> Unit,
    selectedDemographics: List<PublicationDemographic>,
    onDemographicSelected: (PublicationDemographic) -> Unit,
) {
    BackHandler {
        hide()
    }

    val space = LocalSpacing.current
    val groupedTags =
        remember(tagsUiState) {
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
            .systemBarsPadding(),
    ) {
        FilterTopBar {
            hide()
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = space.large)
                .verticalScroll(rememberScrollState()),
        ) {
            Column {
                Row(Modifier.fillMaxWidth()) {
                    AuthorSearchBar(
                        modifier =
                        Modifier
                            .padding(space.small)
                            .weight(1f),
                        label = "Authors",
                        query = authorQuery,
                        onQueryChange = onAuthorQueryChange,
                        result = authorListState,
                        onAuthorSelected = onAuthorSelected,
                        selectedAuthors = selectedAuthors,
                    )
                    AuthorSearchBar(
                        modifier =
                        Modifier
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
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = space.med),
            ) {
                Text("Publication Status")
                FlowRow {
                    statusList.forEach { status ->
                        FilterChip(
                            selected =
                            remember(status, selectedStatus) {
                                selectedStatus.contains(status)
                            },
                            onClick = { onStatusSelected(status) },
                            label = { Text(status.name) },
                            modifier = Modifier.padding(horizontal = space.xs),
                        )
                    }
                }
                Spacer(Modifier.height(space.med))
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = space.med),
            ) {
                val demographics =
                    remember {
                        PublicationDemographic.values().toList()
                    }
                Text("Magazine Demographic")
                FlowRow {
                    demographics.forEach { demographic ->
                        FilterChip(
                            selected =
                            remember(demographic, selectedDemographics) {
                                selectedDemographics.contains(demographic)
                            },
                            onClick = { onDemographicSelected(demographic) },
                            label = { Text(demographic.name) },
                            modifier = Modifier.padding(horizontal = space.xs),
                        )
                    }
                }
                Spacer(Modifier.height(space.med))
            }
            Column(Modifier.padding(horizontal = space.med)) {
                LanguageSelection(
                    label = { Text("Original Language") },
                    placeholder = "All Languages",
                    selected = selectedOriginalLanguages,
                    onLanguageSelected = onOriginalLanguageSelected,
                )
                Spacer(Modifier.height(space.med))
                LanguageSelection(
                    label = { Text("Translated Languages") },
                    placeholder = "All Languages",
                    selected = selectedTranslatedLanguages,
                    onLanguageSelected = onTranslatedLanguageSelected,
                )
                Spacer(Modifier.height(space.med))
            }
            TagsFilterHeader(
                includedTagsMode = includedTagsMode,
                includedTagsModeChanged = includedTagsModeChanged,
                excludedTagsMode = excludedTagsMode,
                excludedTagsModeChanged = excludedTagsModeChanged,
                tagsVisible = tagsVisible,
                tagsVisibleChange = { tagsVisible = it },
                includedChange = { included = it },
                included = included,
            )
            TagsList(
                tagsVisible = tagsVisible,
                included = included,
                groupedTags = groupedTags,
                includedIds = includedIds,
                includedSelected = includedSelected,
                excludedIds = excludedIds,
                excludedSelected = excludedSelected,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelection(
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    placeholder: String,
    selected: List<io.silv.ui.Language>,
    onLanguageSelected: (io.silv.ui.Language) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val maxHeight =
        remember(screenHeightDp) {
            screenHeightDp / 3f
        }
    val space = LocalSpacing.current

    Box(
        modifier =
        modifier
            .wrapContentSize(Alignment.BottomStart),
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            label?.invoke()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = null,
                    )
                }
                if (selected.isEmpty()) {
                    Text(text = placeholder)
                } else {
                    when (selected.size) {
                        in 1..2 -> {
                            selected.forEach {
                                FilterChip(
                                    selected = true,
                                    onClick = { onLanguageSelected(it) },
                                    label = { Text(it.string) },
                                    modifier = Modifier.padding(horizontal = space.xs),
                                )
                            }
                        }
                        else -> {
                            FilterChip(
                                selected = true,
                                onClick = { onLanguageSelected(selected.first()) },
                                label = { Text(selected.first().string) },
                                modifier = Modifier.padding(horizontal = space.xs),
                            )
                            AssistChip(
                                onClick = { expanded = !expanded },
                                enabled = true,
                                label = { Text("+${selected.size - 1} more") },
                                modifier = Modifier.padding(horizontal = space.xs),
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
            io.silv.ui.Language.values().forEach { language ->
                DropdownMenuItem(
                    onClick = { onLanguageSelected(language) },
                    text = { Text(language.string) },
                    leadingIcon = {
                        Image(
                            painter = painterResource(id = language.resId),
                            contentDescription = "flag",
                            modifier = Modifier.size(50.dp),
                            contentScale = ContentScale.Fit,
                        )
                    },
                    trailingIcon = {
                        Checkbox(
                            checked =
                            remember(language, selected) {
                                derivedStateOf { language in selected }.value
                            },
                            onCheckedChange = {
                                onLanguageSelected(language)
                            },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun FilterTopBar(onCloseIconClick: () -> Unit) {
    val space = LocalSpacing.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(space.med),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "Filters", style = MaterialTheme.typography.titleLarge)
        IconButton(onClick = onCloseIconClick) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TagsList(
    tagsVisible: Boolean,
    included: Boolean,
    groupedTags: Map<String, List<DomainTag>>,
    includedIds: List<String>,
    includedSelected: (String) -> Unit,
    excludedIds: List<String>,
    excludedSelected: (String) -> Unit,
) {
    val space = LocalSpacing.current
    Column(Modifier.padding(horizontal = space.med)) {
        AnimatedVisibility(
            visible = tagsVisible,
        ) {
            Column {
                if (included) {
                    groupedTags.forEach { (group, tags) ->
                        Text(
                            text = group,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        FlowRow {
                            tags.forEach {
                                FilterChip(
                                    selected = it.id in includedIds,
                                    onClick = { includedSelected(it.id) },
                                    label = { Text(it.name) },
                                    modifier = Modifier.padding(horizontal = space.xs),
                                )
                            }
                        }
                    }
                } else {
                    groupedTags.forEach { (group, tags) ->
                        Text(
                            text = group,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        FlowRow {
                            tags.forEach {
                                FilterChip(
                                    selected = it.id in excludedIds,
                                    onClick = { excludedSelected(it.id) },
                                    label = { Text(it.name) },
                                    modifier = Modifier.padding(horizontal = space.xs),
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
private fun TagsFilterHeader(
    includedTagsMode: TagsMode,
    includedTagsModeChanged: (TagsMode) -> Unit,
    excludedTagsMode: TagsMode,
    excludedTagsModeChanged: (TagsMode) -> Unit,
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
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TagModeSwitch(
                    title = "Tag inclusion mode",
                    mode = includedTagsMode,
                    onModeChange = {
                        includedTagsModeChanged(it)
                    },
                    modifier = Modifier.weight(1f),
                )
                TagModeSwitch(
                    title = "Tag exclusion mode",
                    mode = excludedTagsMode,
                    onModeChange = {
                        excludedTagsModeChanged(it)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = tagsVisible,
                    onCheckedChange = {
                        tagsVisibleChange(it)
                    },
                )
                Text("show tags")
                IconButton(onClick = { tagsVisibleChange(!tagsVisible) }) {
                    Icon(
                        imageVector =
                        if (tagsVisible) {
                            Icons.Filled.KeyboardArrowUp
                        } else {
                            Icons.Filled.KeyboardArrowDown
                        },
                        contentDescription = null,
                    )
                }
            }
            TagSelectRow(
                modifier =
                Modifier
                    .padding(vertical = space.med)
                    .fillMaxWidth()
                    .height(40.dp),
                included = included,
                onIncludedChange = includedChange,
            )
        }
    }
}

@Composable
private fun AuthorTextField(
    modifier: Modifier = Modifier,
    value: String,
    labelString: String,
    onValueChanged: (String) -> Unit,
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
                fontSize = 12.sp,
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
                        contentDescription = null,
                    )
                }
            }
        },
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
    onAuthorSelected: (author: DomainAuthor) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    val space = LocalSpacing.current
    val focusManager = LocalFocusManager.current
    var currentCoordinates: IntOffset by remember { mutableStateOf(IntOffset(0, 0)) }
    var sizeOffset: IntSize by remember { mutableStateOf(IntSize(0, 0)) }

    val popupPositionProvider =
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                return currentCoordinates.copy(
                    x = currentCoordinates.x + if (addCords) sizeOffset.width else 0,
                    y = currentCoordinates.y + sizeOffset.height,
                )
            }
        }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Bottom,
    ) {
        LaunchedEffect(Unit) {
            snapshotFlow { currentCoordinates.y }.collect {
                if ((it > (0 + sizeOffset.height)) && focused) {
                    focusManager.clearFocus(true)
                }
            }
        }

        AuthorTextField(
            value = query,
            labelString = label,
            onValueChanged = onQueryChange,
            modifier =
            Modifier
                .onGloballyPositioned { layoutCoordinates ->
                    val (x: Int, y: Int) =
                        when {
                            layoutCoordinates.isAttached ->
                                with(layoutCoordinates.positionInRoot()) {
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
                },
        )
        LazyRow {
            items(selectedAuthors) {
                FilterChip(
                    selected = true,
                    onClick = { onAuthorSelected(it) },
                    label = { Text(it.name) },
                    modifier = Modifier.padding(horizontal = space.xs),
                )
            }
        }
        val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
        AnimatedVisibility(
            visible = query.isNotEmpty() && (currentCoordinates.y > 0 + sizeOffset.height),
        ) {
            Popup(
                popupPositionProvider = popupPositionProvider,
                properties = PopupProperties(),
            ) {
                LazyColumn(
                    Modifier
                        .fillMaxWidth(0.5f)
                        .heightIn(0.dp, screenHeightDp / 3)
                        .background(MaterialTheme.colorScheme.surface),
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
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    onClick = { onAuthorSelected(it) },
                                )
                                Divider()
                            }
                        }
                        QueryResult.Loading -> {
                            item {
                                CenterBox(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(space.med),
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

@Composable
private fun TagModeSwitch(
    modifier: Modifier = Modifier,
    title: String,
    mode: TagsMode = TagsMode.AND,
    icon: (@Composable () -> Unit)? = null,
    onModeChange: (TagsMode) -> Unit,
) {
    val space = LocalSpacing.current
    val transformed = remember(mode) { mode == TagsMode.OR }
    Column(
        modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Text(
                text = "AND",
                style =
                MaterialTheme.typography.labelLarge.copy(
                    color =
                    if (transformed) {
                        Color.Unspecified
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    fontWeight =
                    if (transformed) {
                        FontWeight.Normal
                    } else {
                        FontWeight.Bold
                    },
                ),
                modifier = Modifier.padding(horizontal = space.med),
            )
            if (!transformed) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(100))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            3.dp,
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(100),
                        )
                        .padding(space.small),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CenterBox {
                        CenterBox(
                            modifier =
                            Modifier
                                .clip(CircleShape)
                                .size(22.dp)
                                .background(MaterialTheme.colorScheme.primary),
                        ) {
                            icon?.invoke()
                        }
                    }
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
                            RoundedCornerShape(100),
                        )
                        .padding(space.small),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CenterBox {
                        CenterBox(
                            modifier =
                            Modifier
                                .clip(CircleShape)
                                .size(22.dp)
                                .background(MaterialTheme.colorScheme.primary),
                        ) {
                            icon?.invoke()
                        }
                    }
                }
            }
            Text(
                text = "OR",
                style =
                MaterialTheme.typography.labelLarge.copy(
                    color =
                    if (!transformed) {
                        Color.Unspecified
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    fontWeight =
                    if (!transformed) {
                        FontWeight.Normal
                    } else {
                        FontWeight.Bold
                    },
                ),
                modifier = Modifier.padding(horizontal = space.med),
            )
        }
    }
}

@Composable
private fun TagSelectRow(
    modifier: Modifier = Modifier,
    included: Boolean,
    onIncludedChange: (Boolean) -> Unit,
) {
    Column(modifier.noRippleClickable { onIncludedChange(!included) }.fillMaxWidth()) {
        if (included) {
            Box(
                Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                )
                TextItems()
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                )
                TextItems()
            }
        }
    }
}

@Composable
private fun TextItems() {
    Row(
        Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        listOf("Included", "Excluded").forEach {
            Text(
                text = it,
                modifier =
                Modifier
                    .weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
