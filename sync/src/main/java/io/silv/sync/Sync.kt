package io.silv.sync

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import io.silv.sync.workers.SeasonalMangaSyncWorker
import io.silv.sync.workers.TagSyncWorker

object Sync {
    fun init(context: Context) {
        WorkManager.getInstance(context).apply {
            // Run sync on app startup and ensure only one sync worker runs at any time
//            enqueueUniqueWork(
//                MangaSyncWorkName,
//                ExistingWorkPolicy.KEEP,
//                MangaSyncWorker.syncWorkRequest(),
//            )
            enqueueUniqueWork(
                SeasonalMangaSyncWorkName,
                ExistingWorkPolicy.REPLACE,
                SeasonalMangaSyncWorker.syncWorkRequest(),
            )
            enqueueUniqueWork(
                TagSyncWorkName,
                ExistingWorkPolicy.REPLACE,
                TagSyncWorker.syncWorkRequest(),
            )
        }
    }
}
