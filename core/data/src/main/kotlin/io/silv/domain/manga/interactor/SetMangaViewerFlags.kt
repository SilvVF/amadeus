package io.silv.domain.manga.interactor

import io.silv.domain.manga.model.MangaUpdate
import io.silv.domain.manga.repository.MangaRepository

class SetMangaViewerFlags(
    private val mangaRepository: MangaRepository,
) {

    suspend fun awaitSetReadingMode(id: String, flag: Long) {
        TODO()
//        val manga = mangaRepository.getMangaById(id)
//        mangaRepository.updateManga(
//            MangaUpdate(
//                id = id,
//                viewerFlags = manga.viewerFlags.setFlag(flag, ReadingMode.MASK.toLong()),
//            ),
//        )
    }

    suspend fun awaitSetOrientation(id: String, flag: Long) {
        TODO()
    //        val manga = mangaRepository.getMangaById(id)
//        mangaRepository.updateManga(
//            MangaUpdate(
//                id = id,
//                viewerFlags = manga.viewerFlags.setFlag(flag, ReaderOrientation.MASK.toLong()),
//            ),
//        )
    }

    private fun Long.setFlag(flag: Long, mask: Long): Long {
        return this and mask.inv() or (flag and mask)
    }
}