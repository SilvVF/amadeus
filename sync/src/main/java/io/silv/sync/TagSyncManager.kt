package io.silv.sync

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import io.silv.data.OSWorkManagerHelper
import io.silv.sync.workers.TagSyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map

class TagSyncManager(
    private val context: Context,
) : SyncManager {

    val workManager by lazy { OSWorkManagerHelper.getInstance(context) }

    override val isSyncing: Flow<Boolean>
        get() = workManager.getWorkInfosForUniqueWorkFlow(TagSyncWorkName)
            .map(List<WorkInfo>::anyRunning)
            .conflate()

    override fun requestSync() {
        workManager.enqueueUniqueWork(
            TagSyncWorkName,
            ExistingWorkPolicy.KEEP,
            TagSyncWorker.syncWorkRequest(),
        )
    }
}
