package io.silv.data.workers.cover_art

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import io.silv.common.coroutine.suspendRunCatching
import io.silv.data.workers.ImageDownloader
import kotlinx.coroutines.flow.firstOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration

const val CoverArtDownloadWorkerTag = "CoverArtDownloadWorkerTag"

internal class CoverArtDownloadWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
): CoroutineWorker(appContext, workerParameters), KoinComponent {

    private val dispatchers by inject<io.silv.common.AmadeusDispatchers>()
    private val imageDownloader = ImageDownloader(appContext, dispatchers)
    private val savedMangaDao by inject<io.silv.database.dao.SavedMangaDao>()

    override suspend fun doWork(): Result {

        val url = inputData.getString(URL_KEY)
        val mangaId = inputData.getString(MANGA_ID_KEY)

        if (url == null || mangaId == null) {
            return Result.failure()
        }

        val result = suspendRunCatching {
            val uri = imageDownloader.writeMangaCoverArt(mangaId, url)
                .toString()
            savedMangaDao.getSavedMangaById(mangaId).firstOrNull()?.let {
                savedMangaDao.updateSavedManga(it.copy(
                    coverArt = uri
                ))
            }
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