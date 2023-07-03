package io.silv.manga.sync

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map

// This name should not be changed otherwise the app may have concurrent sync requests running
const val MagnaSyncWorkName = "MangaSyncWorkName"

interface SyncManager {
    val isSyncing: Flow<Boolean>
    fun requestSync()
}

/**
 * [SyncManager] backed by [WorkInfo] from [WorkManager]
 */
internal class MangaSyncManger (
    private val context: Context,
) : SyncManager {
    override val isSyncing: Flow<Boolean> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(MagnaSyncWorkName)
            .map(List<WorkInfo>::anyRunning)
            .conflate()

    override fun requestSync() {
        val workManager = WorkManager.getInstance(context)
        // Run sync on app startup and ensure only one sync worker runs at any time
        workManager.enqueueUniqueWork(
            MagnaSyncWorkName,
            ExistingWorkPolicy.KEEP,
            MangaSyncWorker.syncWorkRequest(),
        )
    }
}


private fun List<WorkInfo>.anyRunning() = any { it.state == WorkInfo.State.RUNNING }
