package io.silv.data.workers.chapters

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
import io.silv.common.pForEach
import io.silv.data.workers.createForegroundInfo
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.time.Duration


const val ChapterDeletionWorkerTag = "ChapterDeletionWorkerTag"

class ChapterDeletionWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
): CoroutineWorker(appContext, workerParameters), KoinComponent {


    private val dispatchers by inject<io.silv.common.AmadeusDispatchers>()
    private val chapterDao by inject<io.silv.database.dao.ChapterDao>()

    override suspend fun doWork(): Result {

        val chaptersToDelete = inputData.getStringArray(CHAPTERS_KEY)?.toList()
            ?: return Result.failure()

        val result = runCatching {
            withContext(dispatchers.io) {
                chaptersToDelete.pForEach(this) { id ->
                    val chapter = chapterDao.getChapterById(id) ?: return@pForEach
                    val deleted = chapter.chapterImages.map { uri ->
                        val file = File(uri.toUri().path ?: error("No path for $uri"))
                        file.delete() to uri
                    }
                        .mapNotNull { if (it.first) null else it.second }
                    chapterDao.updateChapter(
                        chapter.copy(
                            chapterImages = deleted
                        )
                    )
                }
            }
        }
            .onFailure {
                it.printStackTrace()
            }

        return if (result.isSuccess)
            Result.success()
        else
            Result.failure()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return applicationContext.createForegroundInfo(
            0, ChapterDeletionWorkerTag
        )
    }

    companion object {
        const val CHAPTERS_KEY = "CHAPTERS_KEY"

        fun deletionWorkRequest(chapterIds: List<String>): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<ChapterDeletionWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    Duration.ofSeconds(5),
                )
                .setInputData(
                    Data.Builder()
                        .putStringArray(CHAPTERS_KEY, chapterIds.toTypedArray())
                        .build()
                )
                .addTag(ChapterDeletionWorkerTag)
                .build()
        }
    }
}