package io.silv.manga.view

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import io.silv.datastore.model.Filters
import io.silv.ui.CenterBox
import io.silv.ui.theme.LocalSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    visible: Boolean,
    filters: Filters,
    sheetState: SheetState,
    onSetAsDefaultClick: () -> Unit,
    filterDownloaded: () -> Unit,
    filterUnread: () -> Unit,
    filterBookmarked: () -> Unit,
    filterBySource: () -> Unit,
    filterByChapterNumber: () -> Unit,
    filterByUploadDate: () -> Unit,
    showSourceTitle: () -> Unit,
    hideSourceTitle: () -> Unit,
    showingSourceTitle: Boolean,
    onDismiss: () -> Unit,
) {
    val space = LocalSpacing.current
    var selectedTabIdx by rememberSaveable {
        mutableIntStateOf(0)
    }
    var dropdownVisible by rememberSaveable {
        mutableStateOf(false)
    }

    if (visible) {
        ModalBottomSheet(
            windowInsets = WindowInsets(0),
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = {},
        ) {
            TabRow(
                selectedTabIndex = selectedTabIdx,
                modifier =
                Modifier
                    .padding(space.small)
                    .fillMaxWidth(),
            ) {
                Tab(
                    selected = selectedTabIdx == 0,
                    onClick = { selectedTabIdx = 0 },
                    text = { Text("Filter") },
                )
                Tab(
                    selected = selectedTabIdx == 1,
                    onClick = { selectedTabIdx = 1 },
                    text = {
                        Text("Sort")
                    },
                )
                Tab(
                    selected = selectedTabIdx == 2,
                    onClick = { selectedTabIdx = 2 },
                    text = {
                        Text("Display")
                    },
                )
                Box {
                    IconButton(onClick = { dropdownVisible = !dropdownVisible }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = null,
                        )
                    }
                    DropdownMenu(
                        expanded = dropdownVisible,
                        onDismissRequest = { dropdownVisible = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Set as default") },
                            onClick = onSetAsDefaultClick,
                        )
                    }
                }
            }
            AnimatedContent(
                targetState = selectedTabIdx,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.25f),
                transitionSpec = {
                    if (initialState < targetState) {
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    } else {
                        slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                    }
                },
                label = "filter",
            ) { idx ->
                when (idx) {
                    0 -> {
                        val items by remember(filters) {
                            derivedStateOf {
                                listOf(
                                    Triple("Downloaded", filters.downloaded, filterDownloaded),
                                    Triple("Unread", filters.unread, filterUnread),
                                    Triple("Bookmarked", filters.bookmarked, filterBookmarked),
                                )
                            }
                        }
                        Column(Modifier.fillMaxWidth()) {
                            items.fastForEach { (text, checked, action) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { action() },
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { action() },
                                        enabled = true,
                                        colors =
                                        CheckboxDefaults.colors(
                                            checkedColor = MaterialTheme.colorScheme.primary,
                                            uncheckedColor = MaterialTheme.colorScheme.onBackground,
                                        ),
                                    )
                                    Text(text)
                                }
                            }
                        }
                    }
                    1 -> {
                        val items by remember(filters) {
                            derivedStateOf {
                                listOf(
                                    Triple("By source", filters.bySourceAsc, filterBySource),
                                    Triple(
                                        "By chapter number",
                                        filters.byChapterAsc,
                                        filterByChapterNumber
                                    ),
                                    Triple(
                                        "By upload date",
                                        filters.byUploadDateAsc,
                                        filterByUploadDate
                                    ),
                                )
                            }
                        }
                        Column(Modifier.fillMaxWidth()) {
                            items.fastForEach { (text, ascending, action) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { action() },
                                ) {
                                    CenterBox(Modifier.size(42.dp)) {
                                        if (ascending != null) {
                                            IconButton(onClick = action) {
                                                Icon(
                                                    imageVector =
                                                    if (ascending) {
                                                        Icons.Filled.ArrowUpward
                                                    } else {
                                                        Icons.Filled.ArrowDownward
                                                    },
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                        }
                                    }
                                    Text(text)
                                }
                            }
                        }
                    }
                    2 -> {
                        val items by remember(showingSourceTitle) {
                            derivedStateOf {
                                listOf(
                                    Triple("Source Title", showingSourceTitle, showSourceTitle),
                                    Triple("Chapter number", !showingSourceTitle, hideSourceTitle),
                                )
                            }
                        }
                        Column(Modifier.fillMaxWidth()) {
                            items.fastForEach { (text, selected, action) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { action() },
                                ) {
                                    RadioButton(
                                        selected = selected,
                                        onClick = action,
                                    )
                                    Text(text)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
