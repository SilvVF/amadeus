package io.silv.manga.download

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.common.model.Download
import io.silv.data.download.DownloadManager
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Stable
data class DownloadItem(val download: Download)

class DownloadQueueScreenModel(
    private val downloadManager: DownloadManager,
) : StateScreenModel<ImmutableList<DownloadItem>>(persistentListOf()) {

    init {
        downloadManager.queueState
            .map { downloads ->
                downloads.filter{ it.status != Download.State.DOWNLOADED }.map { download ->
                    DownloadItem(download = download)
                }
            }.onEach { items ->
                mutableState.update { items.toImmutableList() }
            }
            .launchIn(screenModelScope)
    }

    val isDownloaderRunning = downloadManager.isDownloaderRunning
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun getDownloadStatusFlow() = downloadManager.statusFlow()

    fun getDownloadProgressFlow() = downloadManager.progressFlow()

    fun startDownloads() {
        downloadManager.startDownloads()
    }

    fun pauseDownloads() {
        downloadManager.pauseDownloads()
    }

    fun clearQueue() {
        screenModelScope.launch {
            downloadManager.clearQueue()
        }
    }

    fun reorder(downloads: List<Download>) {
        downloadManager.reorderQueue(downloads)
    }

    fun cancel(downloads: List<Download>) {
        downloadManager.cancelQueuedDownloads(downloads)
    }
}