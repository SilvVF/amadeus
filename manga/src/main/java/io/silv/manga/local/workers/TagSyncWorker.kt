package io.silv.manga.local.workers

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import io.silv.manga.domain.repositorys.tags.TagRepository
import io.silv.manga.sync.Synchronizer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration

internal class TagSyncWorker(
    private val appContext: Context,
    workerParams: WorkerParameters,
): CoroutineWorker(appContext, workerParams), Synchronizer, KoinComponent {

    private val tagRepository by inject<TagRepository>()

    override suspend fun doWork(): Result {
        return if (tagRepository.sync()) {
            Result.success()
        } else {
            Result.failure()
        }
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
                    Duration.ofSeconds(15),
                )
                .setConstraints(SyncConstraints)
                .build()
        }
    }
}