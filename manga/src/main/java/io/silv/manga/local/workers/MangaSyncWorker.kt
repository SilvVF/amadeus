package io.silv.manga.local.workers

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
import io.silv.core.pmap
import io.silv.manga.repositorys.chapter.ChapterEntityRepository
import io.silv.manga.repositorys.manga.SavedMangaRepository
import io.silv.manga.sync.MangaSyncWorkName
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration

internal class MangaSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
): CoroutineWorker(appContext, workerParams), KoinComponent {

    private val savedMangaRepository by inject<SavedMangaRepository>()
    private val chapterInfoRepository by inject<ChapterEntityRepository>()

    override suspend fun doWork(): Result {

        val allSynced = listOf(
            savedMangaRepository,
            chapterInfoRepository
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
            get() = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        fun syncWorkRequest(): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<MangaSyncWorker>()
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