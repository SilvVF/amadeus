package io.silv.manga.manga_view

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.screenModelScope
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.message
import io.silv.data.download.DownloadManager
import io.silv.datastore.UserSettingsStore
import io.silv.datastore.model.Filters
import io.silv.domain.chapter.ChapterHandler
import io.silv.domain.manga.GetCombinedSavableMangaWithChapters
import io.silv.domain.manga.GetMangaStatisticsById
import io.silv.domain.manga.MangaHandler
import io.silv.model.MangaStats
import io.silv.model.SavableChapter
import io.silv.model.SavableManga
import io.silv.model.toResource
import io.silv.ui.EventStateScreenModel
import io.silv.ui.ioCoroutineScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class MangaViewScreenModel(
    getCombinedSavableMangaWithChapters: GetCombinedSavableMangaWithChapters,
    getMangaStatisticsById: GetMangaStatisticsById,
    private val userSettingsStore: UserSettingsStore,
    private val mangaHandler: MangaHandler,
    private val chapterHandler: ChapterHandler,
    private val downloadManager: DownloadManager,
    mangaId: String,
): EventStateScreenModel<MangaViewEvent, MangaViewState>(MangaViewState.Loading) {

    private fun updateSuccess(block: (state: MangaViewState.Success) -> MangaViewState) {
        mutableState.update { state ->
            (state as? MangaViewState.Success)?.let(block) ?: state
        }
    }

    private val mutableFilters = MutableStateFlow(Filters())

    init {
        getCombinedSavableMangaWithChapters.subscribe(mangaId)
            .onEach { (manga, chapters) ->
                mutableState.update {
                    (it.success ?: MangaViewState.Success(manga)).copy(
                        manga = manga,
                        chapters = chapters
                    )
                }
            }
            .catch {
                mutableState.value = MangaViewState.Error(it.localizedMessage ?: "")
            }
            .launchIn(screenModelScope)

        downloadManager.queueState.onEach { downloads ->
            updateSuccess {
                it.copy(
                    downloadingIds = downloads.map { it.chapter.id }.toImmutableList()
                )
            }
        }
            .launchIn(screenModelScope)

        state.map { it.success?.chapters }
            .filterNotNull()
            .combine(
                state.map { it.success?.filters }.filterNotNull()
            ) { chapters, filters ->
                updateSuccess { state ->
                    state.copy(
                        filteredChapters = chapters.applyFilters(filters)
                    )
                }
            }
            .launchIn(screenModelScope)


        mutableFilters.onEach {
            updateSuccess { state -> state.copy(filters = it) }
        }
            .launchIn(screenModelScope)

        state.map { it.success?.manga?.id }.filterNotNull()
            .onEach { id ->
                val response = getMangaStatisticsById.await(id)
                updateSuccess {state ->
                    state.copy(
                        statsUiState =  when(response) {
                            is ApiResponse.Success -> StatsUiState.Success(response.data)
                            is ApiResponse.Failure -> StatsUiState.Error(response.message())
                        }
                    )
                }
            }
            .launchIn(screenModelScope)
    }


    fun toggleLibraryManga(id: String) {
        screenModelScope.launch {
            mangaHandler.addOrRemoveFromLibrary(id)
        }
    }

    fun changeChapterBookmarked(id: String) {
        screenModelScope.launch {
            chapterHandler.toggleChapterBookmarked(id)
                .onSuccess {
                    mutableEvents.send(
                        MangaViewEvent.BookmarkStatusChanged(id, it.bookmarked)
                    )
                }
        }
    }

    fun changeChapterReadStatus(id: String) {
        screenModelScope.launch {
            chapterHandler.toggleReadOrUnread(id)
                .onSuccess {
                    mutableEvents.send(
                        MangaViewEvent.ReadStatusChanged(id, it.read)
                    )
                }
        }
    }


    fun filterDownloaded() {
        mutableFilters.update {
            it.copy(
                downloaded = !it.downloaded
            )
        }
    }

    fun filterByUploadDate() {
        mutableFilters.update {
            if (it.byUploadDateAsc != null) {
                it.copy(
                    byUploadDateAsc = !it.byUploadDateAsc!!
                )
            } else {
                it.copy(
                    bySourceAsc = null,
                    byChapterAsc = null,
                    byUploadDateAsc = true
                )
            }
        }
    }

    fun filterByChapterNumber() {
        mutableFilters.update {
            if (it.byChapterAsc != null) {
                it.copy(
                    byChapterAsc = !it.byChapterAsc!!
                )
            } else {
                it.copy(
                    bySourceAsc = null,
                    byChapterAsc = true,
                    byUploadDateAsc = null
                )
            }
        }
    }

    fun filterBySource() {
        mutableFilters.update {
            if (it.bySourceAsc != null) {
                it.copy(
                    bySourceAsc = !it.bySourceAsc!!
                )
            } else {
                it.copy(
                    bySourceAsc = true,
                    byChapterAsc = null,
                    byUploadDateAsc = null
                )
            }
        }
    }

    fun filterBookmarked() {
        mutableFilters.update {
            it.copy(
                bookmarked = !it.bookmarked
            )
        }
    }

    fun filterUnread() {
        mutableFilters.update {
            it.copy(
                unread = !it.unread
            )
        }
    }

    fun deleteChapterImages(chapterId: String) {
        state.value.success?.let {
            screenModelScope.launch {
                downloadManager.deleteChapters(listOf(it.chapters.firstOrNull { it.id == chapterId }?.toResource() ?: return@launch), it.manga.toResource())
            }
        }
    }

    fun setFilterAsDefault() {
        screenModelScope.launch {
            userSettingsStore.updateDefaultFilter(mutableFilters.value)
        }
    }

    fun downloadChapterImages(chapterId: String) {
        state.value.success?.let {
            ioCoroutineScope.launch {
                Log.d("MangaViewScreenModel", "Calling download chapter images")
                downloadManager.downloadChapters(
                    it.manga.toResource(),
                    listOf(
                        it.chapters.firstOrNull { it.id == chapterId }?.toResource()!!
                    )
                )
            }
        }
    }

    private fun List<SavableChapter>.applyFilters(filters: Filters): ImmutableList<SavableChapter> {
        val sortedChapters: List<SavableChapter> = when  {
            filters.byChapterAsc == true -> sortedBy { it.chapter }
            filters.byChapterAsc == false -> sortedByDescending { it.chapter }
            filters.bySourceAsc == true -> groupBy { it.scanlationGroupToId }
                .mapValues { (_, value) -> value.sortedBy { it.chapter } }.values.flatten()
            filters.bySourceAsc == false -> groupBy { it.scanlationGroupToId }
                .mapValues { (_, value) -> value.sortedByDescending { it.chapter } }.values.flatten()
            filters.byUploadDateAsc == true -> sortedBy { it.createdAt }
            filters.byUploadDateAsc == false -> sortedByDescending { it.createdAt }
            else -> this
        }
        return sortedChapters
            .asSequence()
            .filter { if (filters.bookmarked) it.bookmarked else true }
            .filter { if (filters.downloaded) it.downloaded else true }
            .filter { if (filters.unread) !it.read else true }
            .toImmutableList()
    }
}

@Stable
data class FilterActions(
    val downloaded: () -> Unit,
    val uploadDate: () -> Unit,
    val chapterNumber: () -> Unit,
    val source: () -> Unit,
    val bookmarked: () -> Unit,
    val unread: () -> Unit,
    val setAsDefault: () -> Unit,
)

@Stable
data class ChapterActions(
    val bookmark: (id: String) -> Unit,
    val read: (id: String) -> Unit,
    val download: (id: String) -> Unit,
    val delete: (id: String) -> Unit,
)

@Stable
data class MangaActions(
    val addToLibrary: (id: String) -> Unit,
)

@Stable
@Immutable
sealed interface MangaViewState {
    data object Loading : MangaViewState

    data class Success(
        val manga: SavableManga,
        val downloadingIds: ImmutableList<String> = persistentListOf(),
        val loadingArt: Boolean = false,
        val volumeToArt: ImmutableMap<Int, String> = persistentMapOf(),
        val statsUiState: StatsUiState = StatsUiState.Loading,
        val chapters: ImmutableList<SavableChapter> = persistentListOf(),
        val filteredChapters: ImmutableList<SavableChapter> = persistentListOf(),
        val filters: Filters = Filters()
    ) : MangaViewState {

        val volumeToArtList = this.volumeToArt.toList().toImmutableList()
    }

    data class Error(val message: String): MangaViewState

    val success: Success?
        get() = this as? Success
}

@Stable
sealed interface StatsUiState {
    data object Loading : StatsUiState
    data class Error(val message: String) : StatsUiState
    data class Success(val stats: MangaStats) : StatsUiState
}

sealed interface MangaViewEvent {
    data class FailedToLoadVolumeArt(val message: String): MangaViewEvent
    data class FailedToLoadChapterList(val message: String): MangaViewEvent
    data class BookmarkStatusChanged(val id: String, val bookmarked: Boolean): MangaViewEvent
    data class ReadStatusChanged(val id: String, val read: Boolean): MangaViewEvent
}
