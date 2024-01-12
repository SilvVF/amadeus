package io.silv.sync

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map

class SavedMangaSyncManager(
    private val context: Context,
) : SyncManager {

    override val isSyncing: Flow<Boolean> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(MangaSyncWorkName)
            .map(List<WorkInfo>::anyRunning)
            .conflate()

    override fun requestSync() {

    }
}
