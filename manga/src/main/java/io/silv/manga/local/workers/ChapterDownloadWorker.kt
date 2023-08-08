package io.silv.manga.local.workers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
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
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.ktor_response_mapper.suspendOnFailure
import io.silv.manga.domain.ChapterToChapterEntityMapper
import io.silv.manga.domain.suspendRunCatching
import io.silv.manga.domain.usecase.GetMangaResourcesById
import io.silv.manga.local.dao.ChapterDao
import io.silv.manga.local.dao.SavedMangaDao
import io.silv.manga.local.entity.SavedMangaEntity
import io.silv.manga.local.workers.handlers.AzukiHandler
import io.silv.manga.local.workers.handlers.BiliHandler
import io.silv.manga.local.workers.handlers.ComikeyHandler
import io.silv.manga.local.workers.handlers.MangaHotHandler
import io.silv.manga.local.workers.handlers.MangaPlusHandler
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.requests.ChapterListRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.net.URL
import java.time.Duration

const val ChapterDownloadWorkerTag = "ChapterDownloadWorker"

internal class ImageDownloader(
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

        val ext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "webp" else "png"

        val fileName = "$mangaId-$chapterId-$pageNumber.$ext"

        val file = File(context.filesDir, fileName)

        file.outputStream().buffered().use {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, it)
            } else {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }

        file.toUri()
    }
}

class ChapterDownloadWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
): CoroutineWorker(appContext, workerParameters), KoinComponent {

    private val logTag = ChapterDownloadWorkerTag

    private val dispatchers by inject<AmadeusDispatchers>()
    private val chapterDao by inject<ChapterDao>()
    private val getMangaResourcesById by inject<GetMangaResourcesById>()
    private val savedMangaDao by inject<SavedMangaDao>()
    private val mangaDexApi by inject<MangaDexApi>()

    private val azukiHandler by inject<AzukiHandler>()
    private val mangaPlusHandler by inject<MangaPlusHandler>()
    private val mangaHotHandler by inject<MangaHotHandler>()
    private val comikeyHandler by inject<ComikeyHandler>()
    private val biliHandler by inject<BiliHandler>()

    private val downloader = ImageDownloader(appContext, dispatchers)

    private suspend fun fetchAndSaveChapters(ids: List<String>) {
        Log.d(logTag, "fetching $ids")
        mangaDexApi.getChapterData(
            ChapterListRequest(
                ids = ids
            )
        )
            .suspendOnFailure {
                Log.d(logTag, "fetching failed $ids")
            }
            .getOrThrow()
            .also { list ->
                Log.d(logTag, "fetching successful saving $ids")
                list.data.forEach {
                    chapterDao.upsertChapter(
                        ChapterToChapterEntityMapper.map(it to null)
                    )
                }
                Log.d(logTag, "saved $ids")
            }
    }

    private suspend fun saveMangaIfNotSaved(mangaId: String): Boolean = withContext(dispatchers.io){
        // Create saved manga if it is being downloaded from a resource
        savedMangaDao.getSavedMangaById(mangaId).first() ?: run {
            Log.d(logTag, "Manga was not saved previously saving manga id=$mangaId")
            val resources = getMangaResourcesById(mangaId)
            Log.d(logTag, "Manga resources found idToDaoIds=${resources.map { it.first.id to it.second }}")
            if (resources.isEmpty()) {
                return@withContext false
            }
            savedMangaDao.upsertSavedManga(
                SavedMangaEntity(
                    mangaResource = resources
                        .maxBy { it.first.savedLocalAtEpochSeconds }
                        .first
                )
            )
            Log.d(logTag, "saved manga id=$mangaId")
        }
        true
    }

    override suspend fun doWork(): Result {

        val mangaId = inputData.getString(MANGA_ID)
            ?: return Result.failure().also { Log.d(logTag, "mangaId was null") }
        val chaptersToGet = inputData.getStringArray(CHAPTERS_KEY)?.toList()
            ?: return Result.failure().also { Log.d(logTag, "chaptersToGet was null") }

        Log.d(logTag, "starting work with $mangaId manga id $chaptersToGet chapter ids")

        downloadingIds.update { it + chaptersToGet }

        if(!saveMangaIfNotSaved(mangaId)) {
            Log.d(logTag, "failed to find resource")
            return Result.failure()
        }

        val savedChapters = chapterDao.getChapterEntities().first()

        val unsavedChapterIds = chaptersToGet.filter { cid ->
            cid !in savedChapters.map { it.id }
        }

        if (unsavedChapterIds.isNotEmpty()) {
           fetchAndSaveChapters(unsavedChapterIds)
        }
        val result = suspendRunCatching {
            chaptersToGet.mapNotNull { chapterDao.getChapterById(it).firstOrNull() }
                .forEach { chapter ->
                    val externalUrl = chapter.externalUrl?.replace("\\", "") ?: ""
                    val chapterImageUrls = when {
                        "mangaplus.shueisha" in externalUrl -> {
                            Log.d(logTag, "Trying to get Urls from mangaplus.shueisha")
                            mangaPlusHandler.fetchImageUrls(externalUrl)
                        }
                        "azuki.co" in externalUrl -> {
                            Log.d(logTag, "Trying to get Urls from azuki.co")
                            azukiHandler.fetchImageUrls(externalUrl)
                        }
                        "mangahot.jp" in externalUrl -> {
                            Log.d(logTag, "Trying to get Urls from mangahot.jp")
                            mangaHotHandler.fetchImageUrls(externalUrl)
                        }
                        "bilibilicomics.com" in externalUrl -> {
                            Log.d(logTag, "Trying to get Urls from bilibilicomics.com")
                            biliHandler.fetchImageUrls(externalUrl)
                        }
                        "comikey.com" in externalUrl -> {
                            Log.d(logTag, "Trying to get Urls from comikey.com")
                            comikeyHandler.fetchImageUrls(externalUrl)
                        }
                        externalUrl.isBlank() -> {
                            Log.d(logTag, "Trying to get Urls from mangadex api")
                            val response = mangaDexApi.getChapterImages(chapter.id)
                                .getOrThrow()
                            response.chapter.data.map {
                                "${response.baseUrl}/data/${response.chapter.hash}/$it"
                            }
                        }
                        else -> {
                            Log.d(logTag, "No handler implemented for given external url $externalUrl")
                            return@forEach
                        }
                    }
                    Log.d(logTag, "found urls $chapterImageUrls")
                    chapterImageUrls.chunked(4).forEachIndexed { index, urls ->
                        val uris = urls.mapIndexed { i, url ->
                            downloader.write(mangaId, chapter.id, (index * 4 + i), url).toString()
                        }
                        val prevEntity =
                            chapterDao.getChapterById(chapter.id).first() ?: return@forEachIndexed
                        Log.d(
                            logTag,
                            "updating chapter images with $uris \nchunk#$index, endPage = ${(index * 4) + uris.size}"
                        )
                        chapterDao.updateChapter(
                            prevEntity.copy(
                                chapterImages = prevEntity.chapterImages + uris,
                                pages = (index * 4) + uris.size
                            )
                        )
                    }
                    downloadingIds.update { it - chapter.id }
                }
        }
        return if (result.isSuccess)
            Result.success().also {   Log.d(logTag, "Success") }
        else
            Result.failure().also {   Log.d(logTag, "Failure ${result.exceptionOrNull()?.message}") }
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
                        .build()
                )
                .setConstraints(SyncConstraints)
                .addTag(ChapterDownloadWorkerTag)
                .build()
        }
    }
}
