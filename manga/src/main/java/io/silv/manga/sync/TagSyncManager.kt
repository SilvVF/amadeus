package io.silv.manga.sync

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.silv.manga.local.workers.TagSyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map

/**
 * [SyncManager] backed by [WorkInfo] from [WorkManager]
 */
internal class TagSyncManager(
    private val context: Context,
) : SyncManager {

    override val isSyncing: Flow<Boolean> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(TagSyncWorkName)
            .map(List<WorkInfo>::anyRunning)
            .conflate()

    override fun requestSync() {
        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniqueWork(
            TagSyncWorkName,
            ExistingWorkPolicy.KEEP,
            TagSyncWorker.syncWorkRequest(),
        )
    }
}