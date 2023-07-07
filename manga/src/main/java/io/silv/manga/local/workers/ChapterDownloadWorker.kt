package io.silv.manga.local.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent

class ChapterDownloadWorker(
    private val appContext: Context,
    workerParameters: WorkerParameters,
): CoroutineWorker(appContext, workerParameters), KoinComponent {

    private val tag = "ChapterDownloadWorker"


    override suspend fun doWork(): Result {

        return if (true)
            Result.success()
        else
            Result.failure()
    }

    companion object {

    }
}