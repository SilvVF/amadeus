package io.silv.sync.workers

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import io.silv.common.coroutine.suspendRunCatching
import io.silv.common.model.AutomaticUpdatePeriod
import io.silv.data.updates.MangaUpdateJob
import io.silv.data.workers.createForegroundInfo
import io.silv.sync.MangaSyncWorkName
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

internal class MangaSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val mangaUpdateJob by inject<MangaUpdateJob>()

    override suspend fun doWork(): Result {

        val result = suspendRunCatching {
            mangaUpdateJob.update(false)
        }

        return if (result.isSuccess) {
            Result.success()
        } else {
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return applicationContext.createForegroundInfo(3, MangaSyncWorkName)
    }

    companion object {
        // All sync work needs an internet connectionS
        private val SyncConstraints
            get() =
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

        fun oneTimeWorkRequest() {
            OneTimeWorkRequestBuilder<MangaSyncWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15.seconds.toJavaDuration(),
                )
                .setConstraints(SyncConstraints)
                .build()
        }

        fun syncWorkRequest(updatePeriod: AutomaticUpdatePeriod): PeriodicWorkRequest? {
            return when(updatePeriod){
                AutomaticUpdatePeriod.Off -> null
                else -> {
                    PeriodicWorkRequestBuilder<MangaSyncWorker>(updatePeriod.duration.toJavaDuration())
                        .setBackoffCriteria(
                            BackoffPolicy.EXPONENTIAL,
                            15.seconds.toJavaDuration()
                        )
                        .setConstraints(SyncConstraints)
                        .build()
                }
            }
        }
    }
}
