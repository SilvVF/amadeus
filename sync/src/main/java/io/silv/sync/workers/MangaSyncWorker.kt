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
import io.silv.common.pmap
import io.silv.data.chapter.ChapterRepository
import io.silv.data.manga.MangaRepository
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

    private val mangaRepository by inject<MangaRepository>()
    private val chapterInfoRepository by inject<ChapterRepository>()

    override suspend fun doWork(): Result {
        val allSynced =
            listOf(
                mangaRepository,
                chapterInfoRepository,
            )
                .pmap { repository ->
                    repository.sync()
                }
                .all { successful -> successful }

        return if (allSynced) {
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

        fun syncWorkRequest(): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<MangaSyncWorker>()
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
