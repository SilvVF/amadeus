package io.silv.manga.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import io.silv.manga.domain.repositorys.ChapterInfoRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration

class ChapterInfoSyncWorker(
    private val appContext: Context,
    workerParameters: WorkerParameters
): CoroutineWorker(appContext, workerParameters), Synchronizer, KoinComponent {

    private val chapterInfoRepository by inject<ChapterInfoRepository>()

    override suspend fun doWork(): Result {
        println("ChapterInfoSyncWorker starting sync")
        val mangaId = inputData.getString(MANGA_ID_KEY)

        return if (
            chapterInfoRepository.syncWith(this, params = mangaId)
        ) {
            println("ChapterInfoSyncWorker finish sync success")
            Result.success()
        } else {
            if (runAttemptCount < 3) {
                println("ChapterInfoSyncWorker finish sync retry")
                Result.retry()
            } else {
                println("ChapterInfoSyncWorker finish sync failure")
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

        fun syncWorkRequest(data: Data): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<ChapterInfoSyncWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    Duration.ofSeconds(5),
                )
                .setInputData(data)
                .setConstraints(SyncConstraints)
                .build()
        }
        const val MANGA_ID_KEY = "MANGA_ID_KEY"
    }
}