package io.silv.explore

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import io.silv.common.DependencyAccessor
import io.silv.data.TagRepository
import io.silv.di.dataDeps
import io.silv.model.DomainTag
import io.silv.sync.SyncManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.silv.explore.composables.TagsList
import io.silv.navigation.SharedScreen
import io.silv.navigation.push
import io.silv.sync.syncDependencies
import io.silv.ui.composables.SearchTextField
import io.silv.ui.composables.SearchTopAppBar
import io.silv.ui.layout.TopAppBarWithBottomContent
import io.silv.ui.theme.LocalSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

object ViewAllTagsScreen : Screen {
    private fun readResolve(): Any = this

    @Composable
    override fun Content() {

        val state = viewTagsPresenter()

        ViewAllTags(state)
    }
}

private data class ViewTagsState(
    val loading: Boolean,
    val query: String,
    val tags: Map<String, List<DomainTag>>,
    val events: (ViewTagsEvent) -> Unit,
)

private sealed interface ViewTagsEvent {
    data class SearchChanged(val text: String): ViewTagsEvent
}

@OptIn(DependencyAccessor::class)
@Composable
private fun viewTagsPresenter(
    tagsRepo: TagRepository = dataDeps.tagsRepository,
    syncManager: SyncManager = syncDependencies.tagSyncManager,
): ViewTagsState {

    var query by rememberSaveable { mutableStateOf("") }
    val loading by syncManager.isSyncing.collectAsState(false)

    val filteredTags by produceState(emptyMap()) {
        combine(
            snapshotFlow { query },
            tagsRepo.allTags()
        ) { query, tags ->
            val grouped = withContext(Dispatchers.Default) {
                val queryLower = query.lowercase()
                val filtered = tags.filter { queryLower in it.name.lowercase() }
                filtered.groupBy { it.group }
            }

            value = grouped
        }
            .collect()
    }

    return ViewTagsState(
        loading = loading,
        tags = filteredTags,
        query = query,
    ) { event ->
        when (event) {
            is ViewTagsEvent.SearchChanged -> query = event.text
        }
    }
}

@Composable
private fun ViewTagsTopBar(
    state: ViewTagsState,
) {
    var searching by rememberSaveable { mutableStateOf(false) }
    var alreadyRequestedFocus by rememberSaveable { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    TopAppBar(
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
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
                        searchText = state.query,
                        placeHolder = {
                            Text(text = "Search tags...")
                        },
                        onValueChange = {
                            state.events(ViewTagsEvent.SearchChanged(it))
                        },
                        onSearch = {
                            focusRequester.freeFocus()
                        },
                        focusRequester = focusRequester,
                    )
                } else {
                    Text(
                        text = "Tags"
                    )
                }
            }
        },
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
        },
    )
}

@Composable
private fun ViewAllTags(
    state: ViewTagsState,
    modifier: Modifier = Modifier
) {
    val navigator = LocalNavigator.currentOrThrow
    Scaffold(
        modifier.fillMaxSize(),
        topBar = {
           ViewTagsTopBar(state)
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.loading) {
                LinearProgressIndicator()
            }
            TagsList(
                categoryToTags = state.tags,
                selectedTags = emptyList(),
                onTagSelected = {
                    navigator.push(SharedScreen.MangaFilter(it.name, it.id))
                },
                startExpanded = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}