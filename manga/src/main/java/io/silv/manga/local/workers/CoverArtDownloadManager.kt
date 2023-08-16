package io.silv.manga.local.workers

import androidx.work.WorkManager

class CoverArtDownloadManager (
    private val workManager: WorkManager
) {
    fun deleteCover(
        filename: String
    ) {
        workManager.enqueue(
            CoverArtDeletionWorker.coverArtDeleteWorkRequest(filename)
        )
    }

   fun saveCover(
       mangaId: String,
       url: String
   ) {
       workManager.enqueue(
           CoverArtDownloadWorker.coverArtSaveWorkRequest(mangaId, url)
       )
    }
}