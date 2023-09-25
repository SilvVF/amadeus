package io.silv.manga.local.workers

import android.app.Notification
import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import io.silv.core.AmadeusDispatchers
import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.repositorys.chapter.ChapterEntityRepository
import io.silv.manga.repositorys.chapter.ChapterImageRepository
import io.silv.manga.repositorys.manga.SavedMangaRepository
import io.silv.manga.repositorys.suspendRunCatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration

const val ChapterDownloadWorkerTag = "ChapterDownloadWorker"

class ChapterDownloadWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
): CoroutineWorker(appContext, workerParameters), KoinComponent {

    private val logTag = ChapterDownloadWorkerTag

    private val dispatchers by inject<AmadeusDispatchers>()

    private val savedMangaRepository by inject<SavedMangaRepository>()
    private val chapterRepository by inject<ChapterEntityRepository>()
    private val chapterImageRepository by inject<ChapterImageRepository>()

    private val downloader = ImageDownloader(appContext, dispatchers)

    private suspend fun saveMangaIfNotSaved(mangaId: String): Boolean = withContext(dispatchers.io){
        // Create saved manga if it is being downloaded from a resource
        savedMangaRepository.getSavedManga(mangaId).firstOrNull() ?: run {
            Log.d(logTag, "Manga was not saved previously saving manga id=$mangaId")
            savedMangaRepository.saveManga(mangaId)
        }
        true
    }

    private suspend fun saveImages(
        imageUrls: List<String>,
        chapter: ChapterEntity,
        mangaId: String
    ): Result {
        Log.d(logTag, imageUrls.toString())
        val result = suspendRunCatching {
            withContext(dispatchers.io) {
                imageUrls.chunked(4).forEachIndexed { index, urls ->
                    val uris = urls.mapIndexed { i, url ->
                        downloader.write(mangaId, chapter.id, (index * 4 + i), url).toString()
                    }
                    val prevEntity = chapterRepository.getChapterById(chapter.id).firstOrNull()
                        ?: return@forEachIndexed
                    Log.d(
                        logTag,
                        "updating chapter images with $uris \nchunk#$index, endPage = ${(index * 4) + uris.size}"
                    )
                    chapterRepository.saveChapter(
                        prevEntity.copy(
                            chapterImages = prevEntity.chapterImages + uris,
                            pages = imageUrls.size
                        )
                    )
                    val newProgress = chapter.id to (index * 4 + uris.size) / imageUrls.size.toFloat()
                    downloadingIdToProgress.update {
                        it.map { (id, progress) ->
                            if (id == chapter.id) {
                                newProgress
                            } else {
                                id to progress
                            }
                        }
                    }
                }
            }
        }
        downloadingIdToProgress.update { it.filter { p -> p.first != chapter.id } }
        return if (result.isSuccess) {
            Result.success()
        } else  {
            Result.failure()
        }
    }

    override suspend fun doWork(): Result {

        val mangaId = inputData.getString(MANGA_ID)
            ?: return Result.failure().also { Log.d(logTag, "mangaId was null") }
        val chaptersToGet = inputData.getStringArray(CHAPTERS_KEY)?.toList()
            ?: return Result.failure().also { Log.d(logTag, "chaptersToGet was null") }

        val imageUrls = inputData.getStringArray(IMAGES_URLS_KEY)?.toList()

        Log.d(logTag, "starting work with $mangaId manga id $chaptersToGet chapter ids")

        downloadingIdToProgress.update { it + chaptersToGet.map { c -> c to 0f } }

        if(!saveMangaIfNotSaved(mangaId)) {
            Log.d(logTag, "failed to find resource")
            return Result.failure()
        }

        val savedChapters = chapterRepository.getChapters(mangaId).firstOrNull() ?: emptyList()

        val unsavedChapterIds = chaptersToGet.filter { cid ->
            cid !in savedChapters.map { it.id }
        }

        if (unsavedChapterIds.isNotEmpty()) {
           chapterRepository.saveChapters(unsavedChapterIds)
        }

        if (!imageUrls.isNullOrEmpty()) {
            saveImages(
                imageUrls,
                chapterRepository.getChapterById(
                    chaptersToGet.first()
                )
                    .first() ?: error("cant find chapter"),
                mangaId
            )
        }

        val result = suspendRunCatching {
            chaptersToGet.mapNotNull { chapterRepository.getChapterById(it).firstOrNull() }
                .forEach { chapter ->

                    val externalUrl = chapter.externalUrl?.replace("\\", "") ?: ""

                    val chapterImageUrls = chapterImageRepository
                        .getChapterImageUrls(chapter.id, externalUrl)
                        .getOrThrow()

                    Log.d(logTag, "found urls $chapterImageUrls")
                    chapterImageUrls.chunked(4).forEachIndexed { index, urls ->
                        val uris = urls.mapIndexed { i, url ->
                            downloader.write(mangaId, chapter.id, (index * 4 + i), url).toString()
                        }
                        val prevEntity =
                            chapterRepository.getChapterById(chapter.id).firstOrNull() ?: return@forEachIndexed
                        Log.d(
                            logTag,
                            "updating chapter images with $uris \nchunk#$index, endPage = ${(index * 4) + uris.size}"
                        )
                        chapterRepository.saveChapter(
                            prevEntity.copy(
                                chapterImages = prevEntity.chapterImages + uris,
                                pages = chapterImageUrls.size
                            )
                        )
                        val newProgress = chapter.id to (index * 4 + uris.size) / chapterImageUrls.size.toFloat()
                        downloadingIdToProgress.update {
                            it.map { (id, progress) ->
                                if (id == chapter.id) {
                                   newProgress
                                } else {
                                    id to progress
                                }
                            }
                        }
                    }
                    downloadingIdToProgress.update { it.filter { p -> p.first != chapter.id } }
                }
        }
        return if (result.isSuccess)
            Result.success().also {   Log.d(logTag, "Success") }
        else
            Result.failure().also {   Log.d(logTag, "Failure ${result.exceptionOrNull()?.message}") }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(1, Notification())
    }


    companion object {
        const val MANGA_ID = "MANGA_ID"
        const val CHAPTERS_KEY = "CHAPTERS_KEY"
        const val IMAGES_URLS_KEY = "IMAGES_URLS_KEY"

        val downloadingIdToProgress = MutableStateFlow<List<Pair<String, Float>>>(emptyList())

        // All sync work needs an internet connectionS
        private val SyncConstraints
            get() = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        fun downloadWorkRequest(
            chapterIds: List<String>,
            mangaId: String,
            imageUrls: List<String> = emptyList()
        ): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<ChapterDownloadWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    Duration.ofSeconds(5),
                )
                .setInputData(
                    Data.Builder()
                        .putString(MANGA_ID, mangaId)
                        .putStringArray(CHAPTERS_KEY, chapterIds.toTypedArray())
                        .putStringArray(IMAGES_URLS_KEY, imageUrls.toTypedArray())
                        .build()
                )
                .setConstraints(SyncConstraints)
                .addTag(ChapterDownloadWorkerTag)
                .build()
        }
    }
}
