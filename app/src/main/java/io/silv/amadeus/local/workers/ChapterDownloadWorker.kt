package io.silv.amadeus.local.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import io.silv.amadeus.local.cache.ChapterImageCache
import io.silv.amadeus.local.dao.ChapterDao
import io.silv.amadeus.local.dao.MangaDao
import io.silv.amadeus.local.dao.VolumeDao
import io.silv.amadeus.local.entity.ChapterEntity
import io.silv.amadeus.local.entity.MangaEntity
import io.silv.amadeus.local.entity.VolumeEntity
import io.silv.amadeus.pmapIndexed
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ChapterDownloadWorker(
    private val appContext: Context,
    workerParameters: WorkerParameters,
): CoroutineWorker(appContext, workerParameters), KoinComponent {

    private val mangaDao by inject<MangaDao>()
    private val volumeDao by inject<VolumeDao>()
    private val chapterDao by inject<ChapterDao>()
    private val chapterImageCache by inject<ChapterImageCache>()

    override suspend fun doWork(): Result {

        val images = inputData.getStringArray(imageUrlsKey) ?: return Result.failure()
        val mangaId = inputData.getString(mangaIdKey) ?: return Result.failure()
        val chapterId = inputData.getString(chapterIdKey) ?: return Result.failure()
        val volumeNumber = inputData.getString(volumeNumberKey) ?: return Result.failure()
        val savePermanent = inputData.getBoolean(savePermanentKey, false)

        val prevManga = mangaDao.getMangaById(mangaId)
        val volumeId = volumeNumber + mangaId
        val prevVolume = volumeDao.getVolumeById(volumeId)

        mangaDao.upsertManga(
            prevManga?.copy(
                volumes = prevManga.volumes + volumeId
            )
                ?: MangaEntity(
                    mid = mangaId,
                    volumes = listOf(volumeId)
                )
        )

        volumeDao.upsertVolume(
            prevVolume?.copy(
                chapterIds = prevVolume.chapterIds + chapterId
            ) ?: VolumeEntity(
                vid = volumeId,
                mangaId = mangaId,
                chapterIds = listOf(chapterId)
            )
        )

        val uris = images.pmapIndexed { idx, url ->
            chapterImageCache.write(mangaId, volumeId, chapterId, idx, url).toString()
        }
            .toTypedArray()

        chapterDao.upsertChapter(
            ChapterEntity(
                cid = chapterId,
                volumeId = volumeId,
                uris = uris.toList(),
                permanent = savePermanent
            )
        )


        return Result.success(
            Data.Builder()
                .putStringArray("uris", uris)
                .build()
        )
    }

    companion object {
        const val volumeNumberKey = "volume_number"
        const val chapterIdKey = "chapter_id"
        const val mangaIdKey = "chapter_id"
        const val imageUrlsKey = "image_urls"
        const val savePermanentKey = "save_permanent"
    }
}