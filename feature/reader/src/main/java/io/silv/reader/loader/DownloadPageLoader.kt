package io.silv.reader.loader

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import io.silv.common.model.MangaDexSource
import io.silv.common.model.Page
import io.silv.data.download.DownloadManager
import io.silv.data.chapter.toResource
import io.silv.data.manga.model.Manga
import io.silv.data.manga.model.toResource

internal class DownloadPageLoader(
    private val chapter: ReaderChapter,
    private val manga: Manga,
    private val downloadManager: DownloadManager,
    private val context: Context,
) : PageLoader() {
    private val source = MangaDexSource

    override var isLocal: Boolean = true

    override suspend fun getPages(): List<ReaderPage> {
        return getPagesFromDirectory()
    }

    private fun getPagesFromDirectory(): List<ReaderPage> {
        val pages = downloadManager.buildPageList(
            source,
            manga.toResource(),
            chapter.chapter.toResource()
        )
        return pages.map { page ->
            ReaderPage(page.index, page.url, page.imageUrl) {
                context.contentResolver.openInputStream(page.imageUrl?.toUri() ?: Uri.EMPTY)!!
            }.apply {
                status = Page.State.READY
            }
        }
    }

    override suspend fun loadPage(page: ReaderPage) = Unit
}
