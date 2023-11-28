package io.silv.data.workers.cover_art

import androidx.work.WorkManager


class CoverArtHandler (
    private val workManager: WorkManager
) {
    fun deleteCover(filename: String) {
        workManager.enqueue(
            CoverArtDeletionWorker.coverArtDeleteWorkRequest(filename)
        )
    }

   fun saveCover(mangaId: String, url: String) {
       workManager.enqueue(
           CoverArtDownloadWorker.coverArtSaveWorkRequest(mangaId, url)
       )
   }
}