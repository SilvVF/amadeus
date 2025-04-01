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
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.silv.common.DependencyAccessor
import io.silv.common.coroutine.suspendRunCatching
import io.silv.common.model.AutomaticUpdatePeriod
import io.silv.data.updates.MangaUpdateJob
import io.silv.data.workers.createForegroundInfo
import io.silv.di.dataDeps
import io.silv.sync.MangaSyncPeriodicWorkName
import io.silv.sync.MangaSyncWorkName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

@OptIn(DependencyAccessor::class)
class MangaSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    private val mangaUpdateJob = dataDeps.mangaUpdateJob

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

        val isRunning: Flow<Boolean> = run {
            dataDeps.workManager
                .getWorkInfosByTagFlow(MangaSyncWorkName)
                .map {
                    it.toList()
                        .filterNotNull()
                        .any { it.state == WorkInfo.State.RUNNING }
                }
        }

        fun enqueueOneTimeWork() {
            dataDeps.workManager.enqueue(
                OneTimeWorkRequestBuilder<MangaSyncWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        15.seconds.inWholeSeconds,
                        TimeUnit.SECONDS
                    )
                    .setConstraints(SyncConstraints)
                    .addTag(MangaSyncWorkName)
                    .build()
            )
        }

        fun syncWorkRequest(updatePeriod: AutomaticUpdatePeriod): PeriodicWorkRequest? {
            return when(updatePeriod){
                AutomaticUpdatePeriod.Off -> null
                else -> {
                    PeriodicWorkRequestBuilder<MangaSyncWorker>(
                        updatePeriod.duration.inWholeSeconds,
                        TimeUnit.SECONDS
                    )
                        .setBackoffCriteria(
                            BackoffPolicy.EXPONENTIAL,
                            15.seconds.inWholeSeconds,
                            TimeUnit.SECONDS
                        )
                        .setConstraints(SyncConstraints)
                        .addTag(MangaSyncPeriodicWorkName)
                        .build()
                }
            }
        }
    }
}
