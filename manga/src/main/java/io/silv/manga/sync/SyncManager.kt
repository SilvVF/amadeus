package io.silv.manga.sync

import androidx.work.WorkInfo
import kotlinx.coroutines.flow.Flow

// This name should not be changed otherwise the app may have concurrent sync requests running
const val MagnaSyncWorkName = "MangaSyncWorkName"

interface SyncManager {
    val isSyncing: Flow<Boolean>
    fun requestSync()
}

internal fun List<WorkInfo>.anyRunning() = any { it.state == WorkInfo.State.RUNNING }