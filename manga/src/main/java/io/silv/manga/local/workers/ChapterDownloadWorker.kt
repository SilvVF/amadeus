package io.silv.manga.local.workers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import io.silv.core.AmadeusDispatchers
import io.silv.core.pForEach
import io.silv.core.pmap
import io.silv.ktor_response_mapper.getOrNull
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.local.dao.ChapterDao
import io.silv.manga.local.dao.MangaResourceDao
import io.silv.manga.local.dao.SavedMangaDao
import io.silv.manga.local.entity.SavedMangaEntity
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.common.Tag
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.net.URL
import java.time.Duration

val ChapterDownloadWorkerTag = "ChapterDownloadWorker"

class ChapterDownloadWorker(
    private val appContext: Context,
    workerParameters: WorkerParameters,
): CoroutineWorker(appContext, workerParameters), KoinComponent {


    private val dispatchers by inject<AmadeusDispatchers>()
    private val chapterDao by inject<ChapterDao>()
    private val mangaResourceDao by inject<MangaResourceDao>()
    private val savedMangaDao by inject<SavedMangaDao>()
    private val mangaDexApi by inject<MangaDexApi>()

    override suspend fun doWork(): Result {

        val mangaId = inputData.getString(MANGA_ID) ?: return Result.failure()
        val chaptersToGet = inputData.getStringArray(CHAPTERS_KEY)?.toList() ?: return Result.failure()

        // Create saved manga if it is being downloaded from a resource
        savedMangaDao.getMangaById(mangaId) ?: run {
            val mangaResource = mangaResourceDao.getMangaById(mangaId) ?: return Result.failure()
            SavedMangaEntity(mangaResource).also { savedMangaDao.upsertManga(it) }
        }

        val result = runCatching {
            chaptersToGet.map { id ->
                withContext(dispatchers.io) {
                    // Download request happens by chapter id which should only be available if
                    // the chapter info was already fetched or saved
                    val chapter = chapterDao.getById(id) ?: error("Could not get chapter with id=$id")
                    val response = mangaDexApi.getChapterImages(id).getOrThrow()
                    // $.baseUrl / $QUALITY / $.chapter.hash / $.chapter.$QUALITY[*]
                    val images = response.chapter.data.pmap { chapterImage ->
                        val image = URL(
                            "${response.baseUrl}/data/${response.chapter.hash}/$chapterImage"
                        )
                        val inputStream = image.openStream()
                        val bitmap = BitmapFactory.decodeStream(inputStream)

                        val fileName = "$mangaId-${chapter.id}-$chapterImage"

                        val file = File(appContext.filesDir, fileName)

                        file.outputStream().use {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                        }

                        file.toUri().also {
                            Log.d("ImageCache", "[URI Created] from $chapterImage -> $it")
                        }.toString()
                    }
                    chapterDao.updateChapter(
                        chapter.copy(
                            chapterImages = images
                        )
                    )
                }
            }
        }

        return if (result.isSuccess)
            Result.success()
        else
            Result.failure()
    }

    companion object {
        const val MANGA_ID = "MANGA_ID"
        const val CHAPTERS_KEY = "CHAPTERS_KEY"

        // All sync work needs an internet connectionS
        private val SyncConstraints
            get() = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        fun downloadWorkRequest(chapterIds: List<String>, mangaId: String): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<ChapterDownloadWorker >()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    Duration.ofSeconds(5),
                )
                .setInputData(
                    Data.Builder()
                        .putString(MANGA_ID, mangaId)
                        .putStringArray(CHAPTERS_KEY, chapterIds.toTypedArray())
                        .build()
                )
                .setConstraints(SyncConstraints)
                .addTag(ChapterDownloadWorkerTag)
                .build()
        }
    }
}
