package io.silv.manga.download

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.common.DependencyAccessor
import io.silv.common.model.Download
import io.silv.data.download.DownloadManager
import io.silv.data.download.QItem
import io.silv.di.dataDeps


import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Stable
data class DownloadItem(val download: QItem<Download>)

class DownloadQueueScreenModel @OptIn(DependencyAccessor::class) constructor(
    private val downloadManager: DownloadManager = dataDeps.downloadManager,
) : StateScreenModel<List<DownloadItem>>(emptyList()) {

    init {
        downloadManager.queueState
            .map { downloads ->
                downloads.filter{ it.status != QItem.State.COMPLETED }.map { download ->
                    DownloadItem(download = download)
                }
            }.onEach { items ->
                mutableState.update { items.toList() }
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