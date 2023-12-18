package io.silv.explore.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.util.fastForEach
import io.silv.explore.UiPagedType
import io.silv.explore.UiQueryFilters
import io.silv.ui.layout.TopAppBarWithBottomContent
import io.silv.ui.theme.LocalSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreTopAppBar(
    modifier: Modifier = Modifier,
    selected: UiPagedType,
    scrollBehavior: TopAppBarScrollBehavior,
    onWebClick: () -> Unit,
    onDisplayOptionsClick: () -> Unit,
    onSearch: (query: String) -> Unit,
    onPageTypeSelected: (UiPagedType) -> Unit,
    onFilterClick: () -> Unit,
) {
    val space = LocalSpacing.current

    var searchText by rememberSaveable { mutableStateOf("") }

    var searching by rememberSaveable { mutableStateOf(false) }
    var alreadyRequestedFocus by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    TopAppBarWithBottomContent(
        bottomContent = {
            val filters =
                remember {
                    listOf(
                        Triple("Trending", Icons.Filled.Whatshot, UiPagedType.Popular),
                        Triple("Recent", Icons.Outlined.AutoAwesome, UiPagedType.Latest),
                        Triple("Seasonal", Icons.Filled.CalendarMonth, UiPagedType.Seasonal),
                        Triple(
                            "Filter",
                            Icons.Filled.FilterList,
                            UiPagedType.Query(UiQueryFilters())
                        ),
                    )
                }
            LazyRow {
                filters.fastForEach { (tag, icon, type) ->
                    item(
                        key = type.toString(),
                    ) {
                        FilterChip(
                            modifier = Modifier.padding(space.xs),
                            selected = selected::class == type::class,
                            onClick = {
                                when (type) {
                                    is UiPagedType.Query -> onFilterClick()
                                    else -> onPageTypeSelected(type)
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = icon.name,
                                )
                            },
                            label = {
                                Text(text = tag)
                            },
                        )
                    }
                }
            }
        },
        scrollBehavior = scrollBehavior,
        modifier = modifier,
        title = {
            AnimatedContent(
                targetState = searching,
                transitionSpec = {
                    if (this.targetState) { // animate search text in from top out to top
                        fadeIn() + slideInVertically { -it } togetherWith fadeOut() + slideOutVertically { it }
                    } else { // animate title up from bottom out to bottom
                        fadeIn() + slideInVertically { it } togetherWith fadeOut() + slideOutVertically { -it }
                    }
                },
                label = "search-anim",
            ) { targetState ->
                if (targetState) {
                    LaunchedEffect(Unit) {
                        if (!alreadyRequestedFocus && searching) {
                            focusRequester.requestFocus()
                        } else {
                            alreadyRequestedFocus = false
                        }
                    }
                    SearchTextField(
                        searchText = searchText,
                        onValueChange = { searchText = it },
                        onSearch = {
                            focusRequester.freeFocus()
                            onSearch(it)
                        },
                        focusRequester = focusRequester,
                    )
                } else {
                    Text("Mangadex")
                }
            }
        },
        navigationIcon = {},
        actions = {
            val icon =
                when (searching) {
                    true -> Icons.Filled.SearchOff
                    false -> Icons.Filled.Search
                }
            IconButton(
                onClick = {
                    alreadyRequestedFocus = false
                    searching = !searching
                },
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                )
            }
            IconButton(onClick = onDisplayOptionsClick) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = "Display Options",
                )
            }
            IconButton(
                onClick = onWebClick,
            ) {
                Icon(imageVector = Icons.Filled.Web, contentDescription = null)
            }
        },
    )
}

@Composable
private fun SearchTextField(
    searchText: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onSearch: (query: String) -> Unit,
) {
    OutlinedTextField(
        modifier =
        modifier
            .focusRequester(focusRequester),
        value = searchText,
        textStyle = MaterialTheme.typography.titleMedium,
        placeholder = { Text("Search Manga...") },
        onValueChange = onValueChange,
        colors =
        TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            cursorColor = LocalContentColor.current,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        trailingIcon = {
            AnimatedVisibility(
                visible = searchText.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = { onValueChange("") },
                ) {
                    Icon(imageVector = Icons.Filled.Clear, contentDescription = null)
                }
            }
        },
        maxLines = 1,
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
        keyboardActions =
        KeyboardActions(
            onSearch = {
                onSearch(searchText)
            },
        ),
    )
}
