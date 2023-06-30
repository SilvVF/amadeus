@file:OptIn(ExperimentalMaterial3Api::class)

package io.silv.amadeus.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.silv.amadeus.domain.models.DomainManga
import io.silv.amadeus.ui.theme.LocalSpacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


class HomeTopBarState(
    initialQuery: String,
    initialSuggestions: List<String>,
    private val mangaList: List<DomainManga>,
    private val scope: CoroutineScope?
) {

    var query by mutableStateOf(initialQuery)

    var suggestions by mutableStateOf(initialSuggestions)

    init {
        scope?.launch {
            snapshotFlow { query }.collect { q ->
                val filteredResults =  mangaList.filter {
                    q.lowercase() in it.title.lowercase()
                }
                suggestions = if (filteredResults.size > 5) {
                   filteredResults.take(5).map { it.title }
                } else {
                    filteredResults.map { it.title }
                }
            }
        }
    }

    fun updateQuery(query: String) {
        this.query = query
    }

    companion object {
        object HomeTopBarSaver: Saver<HomeTopBarState, List<String>> {

            override fun SaverScope.save(value: HomeTopBarState): List<String> {
                return listOf(value.query, value.suggestions.toString())
            }

            override fun restore(value: List<String>): HomeTopBarState {
                return HomeTopBarState(
                    initialQuery = value.first(),
                    initialSuggestions = value.last()
                        .removePrefix("[")
                        .removeSuffix("]")
                        .split(","),
                    mangaList = emptyList(),
                    scope = null
                )
            }
        }
    }
}

@Composable
fun rememberHomeTopBarState(
    initialQuery: String = "",
    initialSuggestions: List<String> = listOf("item", "item2", "item3"),
    mangaList: List<DomainManga> = emptyList(),
    scope: CoroutineScope = rememberCoroutineScope()
) = rememberSaveable(mangaList, initialQuery, scope, saver = HomeTopBarState.Companion.HomeTopBarSaver) {
    HomeTopBarState(initialQuery, initialSuggestions, mangaList, scope)
}


@Composable
fun HomeTopBar(
    modifier: Modifier = Modifier,
    topBarState: HomeTopBarState = rememberHomeTopBarState(),
    filterChipClick: () -> Unit = {},
    searchButtonClick: () -> Unit = {}
) {
    val space = LocalSpacing.current

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(space.small)
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(0.8f),
            value = topBarState.query,
            onValueChange = topBarState::updateQuery,
            trailingIcon = {
                IconButton(
                    onClick = searchButtonClick,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = Icons.Filled.Search.name
                    )
                }
                           },
            singleLine = true,
            leadingIcon = {
                Spacer(
                    modifier = Modifier.width(space.xs))
                SuggestionChip(
                    onClick = filterChipClick,
                    label = {
                        Text("filter")
                    },
                    modifier = Modifier.padding(space.small)
                )
            },
            placeholder = { Text("Search Manga...") },
            shape = RoundedCornerShape(12.dp)
        )
    }
}