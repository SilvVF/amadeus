package io.silv.manga.local.workers

import android.content.Context
import androidx.core.net.toUri
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import io.silv.core.AmadeusDispatchers
import io.silv.manga.domain.suspendRunCatching
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.time.Duration

const val CoverArtDownloadWorkerTag = "CoverArtDownloadWorkerTag"
const val CoverArtDeletionWorkerTag = "CoverArtDeletionWorkerTag"


class CoverArtDeletionWorker(
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

class CoverArtDownloadWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
): CoroutineWorker(appContext, workerParameters), KoinComponent {

    private val dispatchers by inject<AmadeusDispatchers>()
    private val imageDownloader = ImageDownloader(appContext, dispatchers)

    override suspend fun doWork(): Result {

        val url = inputData.getString(URL_KEY)
        val mangaId = inputData.getString(MANGA_ID_KEY)

        if (url == null || mangaId == null) {
            return Result.failure()
        }

        val result = suspendRunCatching {
            imageDownloader.writeMangaCoverArt(mangaId, url)
        }

        return if (result.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }


    companion object {
        const val MANGA_ID_KEY = "MANGA_ID_KEY"
        const val URL_KEY = "URL_KEY"

        fun coverArtSaveWorkRequest(mangaId: String, url: String): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<CoverArtDownloadWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    Duration.ofSeconds(5),
                )
                .setInputData(
                    Data.Builder()
                        .putString(MANGA_ID_KEY, mangaId)
                        .putString(URL_KEY, url)
                        .build()
                )
                .addTag(CoverArtDownloadWorkerTag)
                .build()
        }
    }
}