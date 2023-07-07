package io.silv.manga.sync

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import io.silv.manga.local.workers.MangaSyncWorker
import org.koin.core.component.KoinComponent

object Sync {

    fun init(context: Context) {
        WorkManager.getInstance(context).apply {
            // Run sync on app startup and ensure only one sync worker runs at any time
            enqueueUniqueWork(
                MangaSyncWorkName,
                ExistingWorkPolicy.KEEP,
                MangaSyncWorker.syncWorkRequest(),
            )
        }
    }
}

