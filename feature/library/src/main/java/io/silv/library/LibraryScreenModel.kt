@file:OptIn(FlowPreview::class)

package io.silv.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.common.model.Download
import io.silv.data.download.DownloadManager
import io.silv.domain.manga.interactor.GetLibraryMangaWithChapters
import io.silv.domain.manga.model.MangaWithChapters
import io.silv.domain.update.UpdateWithRelations
import io.silv.domain.update.UpdatesRepository
import io.silv.library.state.LibraryError
import io.silv.library.state.LibraryEvent
import io.silv.library.state.LibraryState
import io.silv.ui.EventStateScreenModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


data class UiChapterUpdate(
    val update: UpdateWithRelations,
    val downloaded: Boolean,
    val download: Download?
)

class LibraryScreenModel(
    private val updatesRepository: UpdatesRepository,
    getLibraryMangaWithChapters: GetLibraryMangaWithChapters,
    downloadManager: DownloadManager,
): EventStateScreenModel<LibraryEvent, LibraryState>(LibraryState.Loading) {

    var mangaSearchText by mutableStateOf("")
    private val filteredTagIds = MutableStateFlow(emptySet<String>())

    @OptIn(FlowPreview::class)
    private val debouncedSearch = snapshotFlow { mangaSearchText }
        .debounce(100)
        .distinctUntilChanged()
        .onStart { emit("") }

    private val downloadTrigger = combine(
        downloadManager.queueState,
        downloadManager.cacheChanges,
    ) { _, _ -> Unit }
        .onStart { emit(Unit) }

    private val libraryMangaWithDownloadState = getLibraryMangaWithChapters.subscribe()
        .map { list ->
            list.map {(manga, chapters) ->
                MangaWithChapters(
                    manga = manga,
                    chapters = chapters.map {
                        it.copy(
                            downloaded = withContext(Dispatchers.IO) {
                                downloadManager.isChapterDownloaded(it.title, it.scanlator, manga.titleEnglish)
                            }
                        )
                    }
                        .toImmutableList()
                )
            }
        }

    init {
        combine(
            libraryMangaWithDownloadState,
            debouncedSearch,
            filteredTagIds,
            downloadTrigger
        ) { list, query, tagIds, _ ->

                if (list.isEmpty()) {
                    mutableState.value = LibraryState.Error(LibraryError.NoFavoritedChapters)
                    return@combine
                }

                mutableState.update { state ->
                    (state.success ?: LibraryState.Success()).copy(
                        filteredTagIds = tagIds.toImmutableList(),
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

        combine(
            updatesRepository.observeUpdates(),
            downloadManager.queueState,
            downloadTrigger,
        ) { x, y ,z -> Triple(x, y, z) }
            .onEach { (updates, downloads, _) ->


                mutableState.update { state ->
                    (state.success ?: LibraryState.Success()).copy(
                        updates = updates.map {

                            val downloaded = withContext(Dispatchers.IO) {
                                downloadManager.isChapterDownloaded(it.chapterName, it.scanlator, it.mangaTitle)
                            }

                            UiChapterUpdate(
                                update = it,
                                downloaded = downloaded,
                                download = downloads.find { download -> download.chapter.id == it.chapterId },
                            )
                        }
                            .toImmutableList()
                    )
                }
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
