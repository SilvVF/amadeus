package io.silv.amadeus.local.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.skydoves.whatif.whatIfNotNullWith
import io.silv.amadeus.local.dao.ChapterDao
import io.silv.amadeus.local.dao.MangaDao
import io.silv.amadeus.local.dao.VolumeDao
import io.silv.amadeus.local.entity.TrackingState
import io.silv.amadeus.pmap
import kotlinx.datetime.Clock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File


class CleanupWorker(
    private val appContext: Context,
    workParameters: WorkerParameters
) : CoroutineWorker(appContext, workParameters), KoinComponent {

    private val chapterDao by inject<ChapterDao>()
    private val mangaDao by inject<MangaDao>()
    private val volumeDao by inject<VolumeDao>()

    override suspend fun doWork(): Result {

        val untrackedChapters = chapterDao.getAll().pmap { chapter ->
            val tracked = volumeDao
                .getVolumeById(chapter.volumeId).whatIfNotNullWith(
                    whatIfNot = { _ -> false },
                    whatIf = {
                        mangaDao.getMangaById(it.mangaId)?.let { manga ->
                            manga.trackingState != TrackingState.NotTracked
                        } ?: false
                    }
                )
            if (tracked) { null } else { chapter }
        }
            .filterNotNull()

        val time = Clock.System.now().epochSeconds
        val secondsInDay = 86400

        untrackedChapters.forEach { chapter ->
            if (time - chapter.createdAtEpochSeconds > secondsInDay * 7) {
                chapterDao.deleteChapter(chapter)

                chapter.uris.forEach {
                    val file = File(appContext.filesDir, it)
                    file.delete()
                }
            }
        }

        return Result.success()
    }
}

object CleanupInitializer {

    private val workRequest = OneTimeWorkRequestBuilder<CleanupWorker>()
        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        .build()

    fun init(
        context: Context
    ) {
        WorkManager.getInstance(context).apply {
            // Run sync on app startup and ensure only one sync worker runs at any time
            enqueueUniqueWork(
                CleanupSyncWork,
                ExistingWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}

// This name should not be changed otherwise the app may have concurrent sync requests running
internal const val CleanupSyncWork = "CleanupSyncWorkName"