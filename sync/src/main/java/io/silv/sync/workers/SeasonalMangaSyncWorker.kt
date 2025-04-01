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
import io.silv.common.DependencyAccessor
import io.silv.data.workers.createForegroundInfo
import io.silv.di.dataDeps
import io.silv.domain.manga.repository.SeasonalMangaRepository
import io.silv.sync.SeasonalMangaSyncWorkName
import java.util.concurrent.TimeUnit

@OptIn(DependencyAccessor::class)
internal class SeasonalMangaSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    private val seasonalMangaRepository = dataDeps.seasonalMangaRepository

    override suspend fun doWork(): Result {
        return if (seasonalMangaRepository.sync()) {
            Result.success()
        } else {
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return applicationContext.createForegroundInfo(4, SeasonalMangaSyncWorkName)
    }

    companion object {
        // All sync work needs an internet connection
        private val SyncConstraints
            get() =
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

        fun syncWorkRequest(): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<SeasonalMangaSyncWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15,
                    TimeUnit.SECONDS
                )
                .addTag(SeasonalMangaSyncWorkName)
                .setConstraints(SyncConstraints)
                .build()
        }
    }
}
