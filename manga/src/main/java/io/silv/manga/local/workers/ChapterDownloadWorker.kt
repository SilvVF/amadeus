package io.silv.manga.local.workers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.MimeTypeMap
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
import io.silv.manga.domain.ChapterToChapterEntityMapper
import io.silv.manga.local.dao.ChapterDao
import io.silv.manga.local.dao.MangaResourceDao
import io.silv.manga.local.dao.SavedMangaDao
import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.local.entity.SavedMangaEntity
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.common.Tag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.net.URL
import java.time.Duration

const val ChapterDownloadWorkerTag = "ChapterDownloadWorker"



class ChapterImageCache(
    private val context: Context,
    private val dispatchers: AmadeusDispatchers
) {
    suspend fun write(
        mangaId: String,
        chapterId: String,
        pageNumber: Int,
        url: String
    ): Uri = withContext(dispatchers.io) {

        val image = URL(url)
        val inputStream = image.openStream().buffered()
        val bitmap = BitmapFactory.decodeStream(inputStream)

        val fileName = "$mangaId-$chapterId-$pageNumber.png"

        val file = File(context.filesDir, fileName)

        file.outputStream().buffered().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        file.toUri()
    }
}

class ChapterDownloadWorker(
    private val appContext: Context,
    workerParameters: WorkerParameters,
): CoroutineWorker(appContext, workerParameters), KoinComponent {

    private val dispatchers by inject<AmadeusDispatchers>()
    private val chapterDao by inject<ChapterDao>()
    private val mangaResourceDao by inject<MangaResourceDao>()
    private val savedMangaDao by inject<SavedMangaDao>()
    private val mangaDexApi by inject<MangaDexApi>()

    private val downloader = ChapterImageCache(appContext, dispatchers)

    override suspend fun doWork(): Result {

        val mangaId = inputData.getString(MANGA_ID) ?: return Result.failure()
        val chaptersToGet = inputData.getStringArray(CHAPTERS_KEY)?.toList() ?: return Result.failure()

        // Create saved manga if it is being downloaded from a resource
        savedMangaDao.getMangaById(mangaId) ?: run {
            val mangaResource = mangaResourceDao.getMangaById(mangaId) ?: return Result.failure()
            SavedMangaEntity(mangaResource).also { savedMangaDao.upsertManga(it) }
        }

        chaptersToGet.forEach {
            chapterDao.getById(it) ?: run {
                mangaDexApi.getMangaFeed(mangaId)
                    .getOrThrow()
                    .data.forEach {
                        chapterDao.upsertChapter(
                            ChapterToChapterEntityMapper.map(it to null)
                        )
                    }
            }
        }

        downloadingIds.update {
            it + chaptersToGet
        }

        val result = runCatching {
            chaptersToGet.forEach { id ->
                withContext(dispatchers.io) {
                    // Download request happens by chapter id which should only be available if
                    // the chapter info was already fetched or saved
                    val response = mangaDexApi.getChapterImages(id).getOrThrow()
                    // $.baseUrl / $QUALITY / $.chapter.hash / $.chapter.$QUALITY[*]
                    response.chapter.data.forEachIndexed { i, img ->
                        val url = "${response.baseUrl}/data/${response.chapter.hash}/$img"
                        chapterDao.getById(id)
                            ?.let { e ->
                                chapterDao.updateChapter(
                                    e.copy(
                                        chapterImages = e.chapterImages +
                                                downloader.write(mangaId, id, i, url)
                                                    .toString()
                                    )
                                )
                            }
                        }
                }
                downloadingIds.update { ids ->
                    ids.filter { it != id }
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

        val downloadingIds = MutableStateFlow<List<String>>(emptyList())

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