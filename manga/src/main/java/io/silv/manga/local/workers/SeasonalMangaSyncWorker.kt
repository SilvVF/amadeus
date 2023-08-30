package io.silv.manga.local.workers

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import io.silv.manga.repositorys.manga.SeasonalMangaRepository
import io.silv.manga.repositorys.minus
import io.silv.manga.repositorys.timeNow
import io.silv.manga.repositorys.timeZone
import io.silv.manga.sync.SeasonalMangaSyncWorkName
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration
import kotlin.time.toKotlinDuration

internal class SeasonalMangaSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
): CoroutineWorker(appContext, workerParams), KoinComponent {

    private val seasonalMangaRepository by inject<SeasonalMangaRepository>()

    override suspend fun doWork(): Result {

        seasonalMangaRepository.observeAllMangaResources().firstOrNull()?.let { seasonalMangas ->

            val latestSync = seasonalMangas
                .maxByOrNull { it.savedAtLocal }
                ?.savedAtLocal
                ?.toInstant(timeZone())
                ?.toLocalDateTime(timeZone()) ?: return@let

            if (timeNow() - latestSync < Duration.ofDays(3).toKotlinDuration()) {
                return Result.success()
            }
        }

        return if (seasonalMangaRepository.sync()) {
            Result.success()
        } else {
            Result.failure()
        }
    }


    companion object {
        // All sync work needs an internet connection
        private val SyncConstraints
            get() = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        fun syncWorkRequest(): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<SeasonalMangaSyncWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    Duration.ofSeconds(15),
                )
                .addTag(SeasonalMangaSyncWorkName)
                .setConstraints(SyncConstraints)
                .build()
        }
    }
}