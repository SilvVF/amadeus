package io.silv.sync

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.silv.sync.workers.SeasonalMangaSyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map

class SeasonalMangaSyncManager(
    private val context: Context,
) : SyncManager {
    override val isSyncing: Flow<Boolean> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(SeasonalMangaSyncWorkName)
            .map(List<WorkInfo>::anyRunning)
            .conflate()

    override fun requestSync() {
        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniqueWork(
            SeasonalMangaSyncWorkName,
            ExistingWorkPolicy.KEEP,
            SeasonalMangaSyncWorker.syncWorkRequest(),
        )
    }
}
