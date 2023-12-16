package io.silv.sync.workers

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import io.silv.data.tags.TagRepository
import io.silv.data.workers.createForegroundInfo
import io.silv.sync.TagSyncWorkName
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

internal class TagSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
): CoroutineWorker(appContext, workerParams), KoinComponent {

    private val tagRepository by inject<TagRepository>()

    override suspend fun doWork(): Result {
        return if (tagRepository.sync()) {
            Result.success()
        } else {
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return applicationContext.createForegroundInfo(5, TagSyncWorkName)
    }

    companion object {
        // All sync work needs an internet connectionS
        private val SyncConstraints
            get() = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        fun syncWorkRequest(): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<TagSyncWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15.seconds.toJavaDuration(),
                )
                .setConstraints(SyncConstraints)
                .build()
        }
    }
}