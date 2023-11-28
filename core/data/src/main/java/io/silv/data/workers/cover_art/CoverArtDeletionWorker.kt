package io.silv.data.workers.cover_art

import android.content.Context
import androidx.core.net.toUri
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import io.silv.common.coroutine.suspendRunCatching
import io.silv.data.workers.createForegroundInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Duration

const val CoverArtDeletionWorkerTag = "CoverArtDeletionWorkerTag"

internal class CoverArtDeletionWorker(
    appContext: Context,
    workerParameters: WorkerParameters
): CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {

        val uri = inputData.getString(FILE_NAME_KEY) ?: return Result.failure()

        if (uri.isBlank()) {
            return Result.success()
        }

        val result = suspendRunCatching {
            withContext(Dispatchers.IO) {
                val file = File(uri.toUri().path ?: error("No path for $uri"))
                file.delete()

            }
        }

        return if (result.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return applicationContext.createForegroundInfo(2, CoverArtDeletionWorkerTag)
    }


    companion object {
        const val FILE_NAME_KEY = "FILE_NAME_KEY"

        fun coverArtDeleteWorkRequest(fileName: String): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<CoverArtDeletionWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    Duration.ofSeconds(5),
                )
                .setInputData(
                    Data.Builder()
                        .putString(FILE_NAME_KEY, fileName)
                        .build()
                )
                .addTag(CoverArtDeletionWorkerTag)
                .build()
        }
    }
}