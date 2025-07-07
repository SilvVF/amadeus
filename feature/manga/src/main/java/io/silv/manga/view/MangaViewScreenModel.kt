package io.silv.manga.view

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.screenModelScope
import com.skydoves.sandwich.fold
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

import io.silv.data.chapter.interactor.ChapterHandler
import io.silv.data.chapter.Chapter
import io.silv.data.chapter.toResource
import io.silv.data.manga.interactor.GetMangaWithChapters
import io.silv.data.manga.interactor.MangaHandler
import io.silv.data.manga.model.Manga
import io.silv.data.manga.model.toResource
import io.silv.model.MangaStats
import io.silv.ui.EventScreenModel
import io.silv.ui.ioCoroutineScope
import kotlinx.coroutines.flow.Flow


import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MangaViewScreenModel @OptIn(DependencyAccessor::class) constructor(
    getMangaWithChapters: GetMangaWithChapters = dataDeps.getMangaWithChapters,
    getMangaStatisticsById: GetMangaStatisticsById = dataDeps.getMangaStatisticsById,
    private val mangaHandler: MangaHandler = dataDeps.mangaHandler,
    private val chapterHandler: ChapterHandler = dataDeps.chapterHandler,
    private val downloadManager: DownloadManager = dataDeps.downloadManager,
    private val coverCache: CoverCache = dataDeps.coverCache,
    private val mangaId: String,
) : EventScreenModel<MangaViewEvent>() {

    private val downloadsFlow = downloadManager.queueState

    private val loadingArtFlow = MutableStateFlow(false)
    private val refreshingChaptersFlow = MutableStateFlow(false)
    private val filtersFlow = MutableStateFlow(Filters())

    private val mangaState: Flow<MangaState> = combine(
        getMangaWithChapters.subscribe(mangaId),
        downloadsFlow,
        filtersFlow,
        refreshingChaptersFlow,
        downloadManager.cacheChanges
    ) { (manga, chapters), downloads, filters, refreshing, _ ->

        val chaptersDownloaded = chapters.map { chapter ->
            chapter.copy(
                downloaded = downloadManager
                    .isChapterDownloaded(
                        chapter.title,
                        chapter.scanlator,
                        manga.titleEnglish
                    )
            )
        }

        MangaState.Success(
            manga,
            chaptersDownloaded,
            chaptersDownloaded.applyFilters(filters),
            refreshing
        ) as MangaState
    }.onStart {
        val first = getMangaWithChapters.await(mangaId)
        if (first == null) {
            emit(MangaState.Error("failed to load manga $mangaId"))
        } else if (first.chapters.isEmpty()) {
            refreshChapterList()
        }
    }

    val statsUiState = flow {
        emit(StatsUiState.Loading)
        val stats = getMangaStatisticsById.await(mangaId)
            .fold(
                onSuccess = {
                    StatsUiState.Success(it)
                },
                onFailure = {
                    StatsUiState.Error(it)
                }
            )
        emit(stats)
    }


    val state = combine(
        mangaState,
        loadingArtFlow,
        statsUiState,
        downloadsFlow,
        filtersFlow,
    ) { mangaState, loadingArt, stats, downloads, filters ->
        MangaViewState(
            mangaState = mangaState,
            loadingArt = loadingArt,
            statsUiState = stats,
            downloads = downloads.filter {
                it.data.chapter.id in mangaState.success?.chapters.orEmpty().map { c -> c.id }
            },
            filters = filters
        )
    }
        .stateInUi(MangaViewState())


    fun refreshChapterList() {
        screenModelScope.launch {
            refreshingChaptersFlow.value = true

            chapterHandler.refreshList(mangaId)

            refreshingChaptersFlow.value = false
        }
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
                    trySendEvent(MangaViewEvent.BookmarkStatusChanged(id, it.bookmarked))
                }
        }
    }

    fun changeChapterReadStatus(id: String) {
        screenModelScope.launch {
            chapterHandler.toggleReadOrUnread(id)
                .onSuccess {
                    trySendEvent(MangaViewEvent.ReadStatusChanged(id, it.read))
                }
        }
    }

    fun updateMangaReadingStatus(readingStatus: ReadingStatus) {
        screenModelScope.launch {
            state.value.mangaState.success?.let {
                mangaHandler.updateMangaStatus(it.manga, readingStatus)
            }
        }
    }

    fun filterDownloaded() {
        filtersFlow.update {
            it.copy(
                downloaded = !it.downloaded,
            )
        }
    }

    fun filterByUploadDate() {
        filtersFlow.update {
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
        filtersFlow.update {
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
        filtersFlow.update {
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
        filtersFlow.update {
            it.copy(
                bookmarked = !it.bookmarked,
            )
        }
    }

    fun filterUnread() {
        filtersFlow.update {
            it.copy(
                unread = !it.unread,
            )
        }
    }

    fun deleteChapterImages(chapterId: String) {
        state.value.mangaState.success?.let {
            screenModelScope.launch {
                downloadManager.deleteChapters(
                    listOf(
                        it.chapters.firstOrNull { it.id == chapterId }?.toResource()
                            ?: return@launch
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
        state.value.mangaState.success?.let {
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
            .filter { if (filters.bookmarked) it.bookmarked else true }
            .filter { if (filters.downloaded) it.downloaded else true }
            .filter { if (filters.unread) !it.read else true }
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


data class MangaViewState(
    val loadingArt: Boolean = false,
    val statsUiState: StatsUiState = StatsUiState.Loading,
    val downloads: List<QItem<Download>> = emptyList(),
    val filters: Filters = Filters(),
    val mangaState: MangaState = MangaState.Loading
)

@Stable
@Immutable
sealed interface MangaState {
    data object Loading : MangaState

    data class Success(
        val manga: Manga,
        val chapters: List<Chapter> = emptyList(),
        val filteredChapters: List<Chapter> = emptyList(),
        val refreshingChapters: Boolean = false
    ) : MangaState

    data class Error(val message: String) : MangaState

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
