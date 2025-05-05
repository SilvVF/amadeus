package io.silv.manga.view

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.screenModelScope
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.message
import io.silv.common.DependencyAccessor
import io.silv.common.log.logcat
import io.silv.common.model.Download
import io.silv.common.model.ReadingStatus
import io.silv.data.download.CoverCache
import io.silv.data.download.DownloadManager
import io.silv.data.download.QItem
import io.silv.data.manga.GetMangaStatisticsById
import io.silv.datastore.model.Filters
import io.silv.di.dataDeps
import io.silv.di.downloadDeps
import io.silv.data.chapter.interactor.ChapterHandler
import io.silv.data.chapter.Chapter
import io.silv.data.chapter.toResource
import io.silv.data.manga.interactor.GetMangaWithChapters
import io.silv.data.manga.interactor.MangaHandler
import io.silv.data.manga.model.Manga
import io.silv.data.manga.model.toResource
import io.silv.model.MangaStats
import io.silv.ui.EventStateScreenModel
import io.silv.ui.ioCoroutineScope



import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MangaViewScreenModel @OptIn(DependencyAccessor::class) constructor(
    getMangaWithChapters: GetMangaWithChapters = dataDeps.getMangaWithChapters,
    getMangaStatisticsById: GetMangaStatisticsById = dataDeps.getMangaStatisticsById,
    private val mangaHandler: MangaHandler = dataDeps.mangaHandler,
    private val chapterHandler: ChapterHandler = dataDeps.chapterHandler,
    private val downloadManager: DownloadManager = downloadDeps.downloadManager,
    private val coverCache: CoverCache = dataDeps.coverCache,
    mangaId: String,
) : EventStateScreenModel<MangaViewEvent, MangaViewState>(MangaViewState.Loading) {

    private fun updateSuccess(block: (state: MangaViewState.Success) -> MangaViewState) {
        mutableState.update { state ->
            (state as? MangaViewState.Success)?.let(block) ?: state
        }
    }

    private val mutableFilters = MutableStateFlow(Filters())

    init {
        combine(
            getMangaWithChapters.subscribe(mangaId).onStart {
                val first = getMangaWithChapters.await(mangaId)
                if (first == null) {
                    mutableState.update { MangaViewState.Error("failed to get manga") }
                } else {
                    if (first.chapters.isEmpty()) {
                        mutableState.update { MangaViewState.Success(first.manga) }
                        refreshChapterList()
                    }
                }
            },
            downloadManager.cacheChanges,
            downloadManager.queueState
        ) { (manga, chapters), _, _ ->
            mutableState.update {
                (it.success ?: MangaViewState.Success(manga)).copy(
                    manga = manga,
                    chapters = chapters.map { chapter ->
                        chapter.copy(
                            downloaded = downloadManager
                                .isChapterDownloaded(chapter.title, chapter.scanlator, manga.titleEnglish)
                        )
                    }
                        .toList(),
                )
            }
        }
            .catch { mutableState.value = MangaViewState.Error(it.localizedMessage ?: "") }
            .launchIn(screenModelScope)

        downloadManager.queueState.onEach { downloads ->
            updateSuccess { state ->
                state.copy(
                    downloads = downloads.filter {
                        it.data.chapter.id in state.chapters.map { c -> c.id }
                    }
                        .toList(),
                )
            }
        }
            .launchIn(screenModelScope)

        state.map { it.success?.chapters }
            .filterNotNull()
            .combine(
                state.map { it.success?.filters }.filterNotNull(),
            ) { chapters, filters ->
                updateSuccess { state ->
                    state.copy(
                        filteredChapters = chapters.applyFilters(filters),
                    )
                }
            }
            .launchIn(screenModelScope)

        mutableFilters.onEach {
            updateSuccess { state -> state.copy(filters = it) }
        }
            .launchIn(screenModelScope)

        state.map { it.success?.manga?.id }
            .filterNotNull()
            .distinctUntilChanged()
            .onEach { id ->
                val response = getMangaStatisticsById.await(id)
                updateSuccess { state ->
                    state.copy(
                        statsUiState =
                        when (response) {
                            is ApiResponse.Success -> StatsUiState.Success(response.data)
                            is ApiResponse.Failure -> StatsUiState.Error(response.message())
                        },
                    )
                }
            }
            .launchIn(screenModelScope)
    }

    fun refreshChapterList() {
        screenModelScope.launch {
            state.value.success?.let {

                updateSuccess {
                    it.copy(refreshingChapters = true)
                }

                chapterHandler.refreshList(it.manga.id)

                updateSuccess {
                    it.copy(
                        refreshingChapters = false
                    )
                }
            }
        }
    }

    fun toggleLibraryManga(id: String) {
        screenModelScope.launch {
            mangaHandler.addOrRemoveFromLibrary(id)
                .onSuccess {

                    if (!it.inLibrary) {

                        ioCoroutineScope.launch {
                            coverCache.deleteFromCache(it.toResource(), true)
                        }
                    }
                }
        }
    }

    fun changeChapterBookmarked(id: String) {
        screenModelScope.launch {
            chapterHandler.toggleChapterBookmarked(id)
                .onSuccess {
                    sendEvent(MangaViewEvent.BookmarkStatusChanged(id, it.bookmarked))
                }
        }
    }

    fun changeChapterReadStatus(id: String) {
        screenModelScope.launch {
            chapterHandler.toggleReadOrUnread(id)
                .onSuccess {
                    sendEvent(MangaViewEvent.ReadStatusChanged(id, it.read))
                }
        }
    }

    fun updateMangaReadingStatus(readingStatus: ReadingStatus) {
        screenModelScope.launch {
            state.value.success?.let {
                mangaHandler.updateMangaStatus(it.manga, readingStatus)
            }
        }
    }

    fun filterDownloaded() {
        mutableFilters.update {
            it.copy(
                downloaded = !it.downloaded,
            )
        }
    }

    fun filterByUploadDate() {
        mutableFilters.update {
            if (it.byUploadDateAsc != null) {
                it.copy(
                    byUploadDateAsc = !it.byUploadDateAsc!!,
                )
            } else {
                it.copy(
                    bySourceAsc = null,
                    byChapterAsc = null,
                    byUploadDateAsc = true,
                )
            }
        }
    }

    fun filterByChapterNumber() {
        mutableFilters.update {
            if (it.byChapterAsc != null) {
                it.copy(
                    byChapterAsc = !it.byChapterAsc!!,
                )
            } else {
                it.copy(
                    bySourceAsc = null,
                    byChapterAsc = true,
                    byUploadDateAsc = null,
                )
            }
        }
    }

    fun filterBySource() {
        mutableFilters.update {
            if (it.bySourceAsc != null) {
                it.copy(
                    bySourceAsc = !it.bySourceAsc!!,
                )
            } else {
                it.copy(
                    bySourceAsc = true,
                    byChapterAsc = null,
                    byUploadDateAsc = null,
                )
            }
        }
    }

    fun filterBookmarked() {
        mutableFilters.update {
            it.copy(
                bookmarked = !it.bookmarked,
            )
        }
    }

    fun filterUnread() {
        mutableFilters.update {
            it.copy(
                unread = !it.unread,
            )
        }
    }

    fun deleteChapterImages(chapterId: String) {
        state.value.success?.let {
            screenModelScope.launch {
                downloadManager.deleteChapters(
                    listOf(
                        it.chapters.firstOrNull { it.id == chapterId }?.toResource() ?: return@launch
                    ),
                    it.manga.toResource(),
                )
            }

        }
    }


    fun pauseDownload(download: Download) {
        screenModelScope.launch {
            downloadManager.pauseDownloads()
        }
    }

    fun resumeDownloads() {
        screenModelScope.launch {
            downloadManager.startDownloads()
        }
    }

    fun cancelDownload(download: Download) {
        screenModelScope.launch {
            downloadManager.cancelQueuedDownloads(listOf(download))
        }
    }

    fun downloadChapterImages(chapterId: String) {
        state.value.success?.let {
            ioCoroutineScope.launch {
                logcat { "Calling download chapter images" }
                downloadManager.downloadChapters(
                    it.manga.toResource(),
                    listOf(
                        it.chapters.firstOrNull { it.id == chapterId }?.toResource()!!,
                    ),
                )
            }
        }
    }

    private fun List<Chapter>.applyFilters(filters: Filters): List<Chapter> {
        val sortedChapters: List<Chapter> =
            when {
                filters.byChapterAsc == true -> sortedBy { it.chapter }
                filters.byChapterAsc == false -> sortedByDescending { it.chapter }
                filters.bySourceAsc == true ->
                    groupBy { it.scanlationGroupToId }
                        .mapValues { (_, value) -> value.sortedBy { it.chapter } }.values.flatten()
                filters.bySourceAsc == false ->
                    groupBy { it.scanlationGroupToId }
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
            .toList()
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
    val refresh: () -> Unit
)

@Stable
data class DownloadActions(
    val pause: (download: Download) -> Unit,
    val cancel: (download: Download) -> Unit,
)

@Stable
data class MangaActions(
    val addToLibrary: (id: String) -> Unit,
    val changeStatus: (ReadingStatus) -> Unit
)

@Stable
@Immutable
sealed interface MangaViewState {
    data object Loading : MangaViewState

    data class Success(
        val manga: Manga,
        val downloads: List<QItem<Download>> = emptyList(),
        val loadingArt: Boolean = false,
        val statsUiState: StatsUiState = StatsUiState.Loading,
        val chapters: List<Chapter> = emptyList(),
        val filteredChapters: List<Chapter> = emptyList(),
        val filters: Filters = Filters(),
        val refreshingChapters: Boolean = false
    ) : MangaViewState

    data class Error(val message: String) : MangaViewState

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
    data class FailedToLoadVolumeArt(val message: String) : MangaViewEvent

    data class FailedToLoadChapterList(val message: String) : MangaViewEvent

    data class BookmarkStatusChanged(val id: String, val bookmarked: Boolean) : MangaViewEvent

    data class ReadStatusChanged(val id: String, val read: Boolean) : MangaViewEvent
}
