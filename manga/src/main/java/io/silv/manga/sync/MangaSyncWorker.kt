package io.silv.manga.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import io.silv.manga.domain.repositorys.MangaRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration

internal class MangaSyncWorker(
    private val appContext: Context,
    workerParams: WorkerParameters,
): CoroutineWorker(appContext, workerParams), Synchronizer, KoinComponent {

    private val mangaRepository by inject<MangaRepository>()

    override suspend fun doWork(): Result {
        val synced = mangaRepository.sync()
        return if (synced) {
            Result.success()
        } else {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }


    companion object {
        // All sync work needs an internet connectionS
        private val SyncConstraints
            get() = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        fun syncWorkRequest(): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<MangaSyncWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    Duration.ofSeconds(5),
                )
                .setConstraints(SyncConstraints)
                .build()
        }
    }
}