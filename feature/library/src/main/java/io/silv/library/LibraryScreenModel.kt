@file:OptIn(FlowPreview::class)

package io.silv.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.domain.manga.interactor.GetLibraryMangaWithChapters
import io.silv.library.state.LibraryError
import io.silv.library.state.LibraryEvent
import io.silv.library.state.LibraryState
import io.silv.ui.EventStateScreenModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class LibraryScreenModel(
    getLibraryMangaWithChapters: GetLibraryMangaWithChapters
): EventStateScreenModel<LibraryEvent, LibraryState>(LibraryState.Loading) {

    var mangaSearchText by mutableStateOf("")
    private val filteredTagIds = MutableStateFlow(emptySet<String>())

    @OptIn(FlowPreview::class)
    private val debouncedSearch = snapshotFlow { mangaSearchText }
        .debounce(100)
        .distinctUntilChanged()
        .onStart { emit("") }

    init {
        combine(
            getLibraryMangaWithChapters.subscribe(),
            debouncedSearch,
            filteredTagIds
        ) { x, y, z -> Triple(x, y, z) }
            .onEach { (list, query, tagIds) ->

                if (list.isEmpty()) {
                    mutableState.value = LibraryState.Error(LibraryError.NoFavoritedChapters)
                    return@onEach
                }

                val filtered = list.filter { (manga, _) ->
                    query.isBlank() || (manga.alternateTitles.values + manga.titleEnglish)
                        .any { title -> query in title }
                }
                    .filter { (manga, _) -> tagIds.isEmpty() || tagIds.any { manga.tagIds.contains(it) } }
                    .toImmutableList()

                mutableState.update { state ->
                    (state.success ?: LibraryState.Success(persistentListOf())).copy(
                        filteredTagIds = tagIds.toImmutableList(),
                        filteredMangaWithChapters = filtered,
                        filteredText = query,
                        mangaWithChapters = list.toImmutableList()
                    )
                }
            }
            .catch {
                mutableState.value = LibraryState.Error(
                    LibraryError.Generic(it.message ?: "unknown err")
                )
            }
            .launchIn(screenModelScope)
    }

    fun onTagFiltered(id: String) {
        screenModelScope.launch {
            filteredTagIds.update { set ->
                set.toMutableSet().apply {
                    if(!add(id)) { remove(id) }
                }
            }
        }
    }

    fun onSearchChanged(text: String) {
        mangaSearchText = text
    }
}
