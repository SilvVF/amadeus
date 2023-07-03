package io.silv.manga.local.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.silv.manga.local.cache.ChapterImageCache
import io.silv.manga.local.dao.ChapterDao
import io.silv.manga.local.dao.MangaDao
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ChapterDownloadWorker(
    private val appContext: Context,
    workerParameters: WorkerParameters,
): CoroutineWorker(appContext, workerParameters), KoinComponent {

    private val tag = "ChapterDownloadWorker"

    private val mangaDao by inject<MangaDao>()
    private val chapterDao by inject<ChapterDao>()
    private val chapterImageCache by inject<ChapterImageCache>()


    private fun log(message: String) {
        Log.d(tag, message)
    }

    private inline fun <reified T> T.log(): T {
        log(this.toString())
        return this
    }

    override suspend fun doWork(): Result {

//        val fetchImages = inputData.getBoolean(fetchImagesKey, true)
//
//        val mangaId = inputData.getString(mangaIdKey).log() ?: return Result.failure().log()
//        val chapterId = inputData.getString(chapterIdKey).log() ?: return Result.failure().log()
//        val volumeNumber = inputData.getString(volumeNumberKey).log() ?: return Result.failure().log()
//
//        val savePermanent = inputData.getBoolean(savePermanentKey, false).log()
//
//        val images = inputData.getStringArray(imageUrlsKey).log() ?:  return Result.failure().log()
//
//
//        val prevManga = mangaDao.getMangaById(mangaId)
//        val volumeId = volumeNumber + mangaId
//        val prevVolume = volumeDao.getVolumeById(volumeId)
//
//        mangaDao.upsertManga(
//            prevManga?.copy(
//                volumes = prevManga.volumes + volumeId
//            )
//                ?: MangaEntity(
//                    mid = mangaId,
//                    volumes = listOf(volumeId)
//                )
//        )
//
//        volumeDao.upsertVolume(
//            prevVolume?.copy(
//                chapterIds = prevVolume.chapterIds + chapterId
//            ) ?: VolumeEntity(
//                vid = volumeId,
//                mangaId = mangaId,
//                chapterIds = listOf(chapterId),
//            )
//        )
//
//        val uris = images.pmapIndexed { idx, url ->
//            chapterImageCache.write(mangaId, volumeId, chapterId, idx, url).toString()
//        }
//            .toTypedArray().log()
//
//        chapterDao.upsertChapter(
//            ChapterEntity(
//                cid = chapterId,
//                volumeId = volumeId,
//                uris = uris.toList(),
//                permanent = savePermanent
//            )
//        )
//
//
//        return Result.success(
//            Data.Builder()
//                .putStringArray("uris", uris)
//                .build()
//        ).log()
        return Result.failure()
    }

    companion object {
        const val fetchImagesKey = "fetch_images"
        const val volumeNumberKey = "volume_number"
        const val chapterIdKey = "chapter_id"
        const val mangaIdKey = "manga_id"
        const val imageUrlsKey = "image_urls"
        const val savePermanentKey = "save_permanent"
    }
}