package io.silv.sync

import androidx.work.WorkInfo
import kotlinx.coroutines.flow.Flow

// This name should not be changed otherwise the app may have concurrent sync requests running
const val MangaSyncWorkName = "MangaSyncWorkName"
const val TagSyncWorkName = "TagSyncWorkName"
const val SeasonalMangaSyncWorkName = "SeasonalMangaSyncWorkName"


interface SyncManager {
    val isSyncing: Flow<Boolean>

    fun requestSync()
}


fun List<WorkInfo>.anyRunning() = any { it.state == WorkInfo.State.RUNNING }
