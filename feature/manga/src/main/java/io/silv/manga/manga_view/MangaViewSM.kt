package io.silv.manga.manga_view

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import cafe.adriel.voyager.core.model.screenModelScope
import com.skydoves.sandwich.message
import com.skydoves.sandwich.suspendOnFailure
import com.skydoves.sandwich.suspendOnSuccess
import io.silv.common.filterUnique
import io.silv.common.model.ProgressState
import io.silv.data.chapter.ChapterEntityRepository
import io.silv.data.manga.SavedMangaRepository
import io.silv.data.workers.chapters.ChapterDeletionWorker
import io.silv.data.workers.chapters.ChapterDownloadWorker
import io.silv.datastore.UserSettingsStore
import io.silv.datastore.model.Filters
import io.silv.domain.GetCombinedSavableMangaWithChapters
import io.silv.domain.GetMangaStatisticsById
import io.silv.model.SavableChapter
import io.silv.model.SavableManga
import io.silv.sync.anyRunning
import io.silv.ui.EventScreenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant


class MangaViewSM(
    getCombinedSavableMangaWithChapters: GetCombinedSavableMangaWithChapters,
    getMangaStatisticsById: GetMangaStatisticsById,
    private val userSettingsStore: UserSettingsStore,
    private val savedMangaRepository: SavedMangaRepository,
    private val chapterEntityRepository: ChapterEntityRepository,
    private val workManager: WorkManager,
    private val mangaId: String,
): EventScreenModel<MangaViewEvent>() {

    init {
        screenModelScope.launch {
            userSettingsStore.observeDefaultFilter().collect {
                mutableFilters.update { it }
            }
        }
    }

    val statsUiState = flow {
        emit(StatsUiState(loading = true))
        getMangaStatisticsById(mangaId)
            .suspendOnFailure {
                emit(StatsUiState(error = message()))
            }
            .suspendOnSuccess { emit(StatsUiState(data = data)) }
    }
        .stateInUi(StatsUiState(loading = true))


    val downloadingOrDeleting = combine(
        workManager.getWorkInfosByTagFlow(io.silv.data.workers.chapters.ChapterDownloadWorkerTag)
            .map { it.anyRunning() },
        workManager.getWorkInfosByTagFlow(io.silv.data.workers.chapters.ChapterDeletionWorkerTag)
            .map { it.anyRunning() },
        ChapterDownloadWorker.downloadingIdToProgress
    ) { downloading, deleting, idsToProgress ->
        if (downloading || deleting) {
            idsToProgress
        } else {
            emptyList()
        }
    }
        .stateInUi(emptyList())

    private val mutableFilters = MutableStateFlow(Filters())

    val mangaViewStateUiState = combine(
        getCombinedSavableMangaWithChapters(mangaId),
        mutableFilters,
    ) { combinedSavableMangaWithChapters, filters ->
            combinedSavableMangaWithChapters.savableManga?.let { manga ->
                val chapters = combinedSavableMangaWithChapters
                        .chapters.map { SavableChapter(it) }
                MangaViewState.Success(
                    loadingArt = false,
                    manga = manga,
                    volumeToArt = manga.volumeToCoverArtUrl.mapKeys { (k, _) ->
                        k.toIntOrNull() ?: 0
                    },
                    filters = filters,
                    chapters = chapters.applyFilters(filters),
                )
            } ?: MangaViewState.Loading
    }
        .stateInUi(MangaViewState.Loading)

    fun bookmarkManga(id: String) {
        screenModelScope.launch {
            savedMangaRepository.bookmarkManga(id)
        }
    }

    fun changeChapterBookmarked(id: String) {
        screenModelScope.launch {
            var new = false
            chapterEntityRepository.updateChapter(id) { entity ->
                entity.copy(
                    bookmarked = !entity.bookmarked.also { new = it }
                )
            }
            mutableEvents.send(
                MangaViewEvent.BookmarkStatusChanged(id, new)
            )
        }
    }

    fun changeChapterReadStatus(id: String) {
        screenModelScope.launch {
            var new = false
            chapterEntityRepository.updateChapter(id) { entity ->
                entity.copy(
                    progressState = when (entity.progressState) {
                        ProgressState.Finished -> ProgressState.NotStarted.also { new = false }
                        ProgressState.NotStarted, ProgressState.Reading -> ProgressState.Finished.also {
                            new = true
                        }
                    }
                )
            }
            mutableEvents.send(
                MangaViewEvent.ReadStatusChanged(id, new)
            )
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

    fun deleteChapterImages(chapterIds: List<String>) {
        screenModelScope.launch {
            workManager.enqueue(
               ChapterDeletionWorker.deletionWorkRequest(chapterIds)
            )
        }
    }

    fun setFilterAsDefault() {
        screenModelScope.launch {
            userSettingsStore.updateDefaultFilter(mutableFilters.value)
        }
    }

    fun downloadChapterImages(chapterIds: List<String>) {
        screenModelScope.launch {
            workManager.enqueueUniqueWork(
                chapterIds.toString(),
                ExistingWorkPolicy.KEEP,
                ChapterDownloadWorker.downloadWorkRequest(
                    chapterIds,
                    mangaId
                )
            )
        }
    }

    private fun List<SavableChapter>.applyFilters(filters: Filters) =
        this.filterUnique { it.id }
            .groupBy {
                if (filters.bySourceAsc != null) {
                    it.scanlationGroupToId
                } else {
                    it.volume
                }
            }
            .mapValues { (_, chapters) ->
                when {
                    filters.byChapterAsc == true ||
                    filters.bySourceAsc == true ->
                        chapters.sortedBy { it.chapter }

                    filters.byChapterAsc == false ||
                    filters.bySourceAsc == false ->
                        chapters.sortedByDescending { it.chapter }
                    else -> chapters
                }
            }
            .flatMap { (_, v) -> v }
            .run {
                if(filters.byChapterAsc == false) {
                    reversed()
                } else {
                    this
                }
            }
            .filter { if (filters.bookmarked) it.bookmarked else true }
            .filter { if (filters.downloaded) it.downloaded else true }
            .filter { if (filters.unread) !it.read else true }
            .run {
                when (filters.byUploadDateAsc) {
                    true -> sortedBy { it.createdAt.epochSeconds() }
                    false -> sortedByDescending { it.createdAt.epochSeconds() }
                    else -> this
                }
            }

    private fun LocalDateTime.epochSeconds() = toInstant(TimeZone.currentSystemDefault()).epochSeconds
}


@Stable
@Immutable
sealed class MangaViewState(
    open val chapters: List<SavableChapter> = emptyList(),
    open val filters: Filters,
) {
    data object Loading : MangaViewState(emptyList(), Filters())

    data class Success(
        val loadingArt: Boolean,
        val volumeToArt: Map<Int, String>,
        val manga: SavableManga,
        override val chapters: List<SavableChapter>,
        override val filters: Filters
    ) : MangaViewState(chapters, filters)


    val success: Success?
        get() = this as? Success
}

@Stable
@Immutable
data class StatsUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val data: io.silv.domain.MangaStats = io.silv.domain.MangaStats()
)

sealed interface MangaViewEvent {
    data class FailedToLoadVolumeArt(val message: String): MangaViewEvent
    data class FailedToLoadChapterList(val message: String): MangaViewEvent
    data class BookmarkStatusChanged(val id: String, val bookmarked: Boolean): MangaViewEvent
    data class ReadStatusChanged(val id: String, val read: Boolean): MangaViewEvent
}
