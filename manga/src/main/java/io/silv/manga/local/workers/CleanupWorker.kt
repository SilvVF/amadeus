package io.silv.manga.local.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent


class CleanupWorker(
    private val appContext: Context,
    workParameters: WorkerParameters
) : CoroutineWorker(appContext, workParameters), KoinComponent {

    override suspend fun doWork(): Result {



        return Result.success()
    }
}

object CleanupInitializer {

    private val workRequest = OneTimeWorkRequestBuilder<CleanupWorker>()
        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        .build()

    fun init(
        context: Context
    ) {
        WorkManager.getInstance(context).apply {
            // Run sync on app startup and ensure only one sync worker runs at any time
            enqueueUniqueWork(
                CleanupSyncWork,
                ExistingWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}

// This name should not be changed otherwise the app may have concurrent sync requests running
internal const val CleanupSyncWork = "CleanupSyncWorkName"